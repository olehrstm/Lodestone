package de.ole101.lodestone.reactive

import de.ole101.lodestone.exceptionManager
import java.util.*

internal enum class Flag { CLEAN, CHECK, DIRTY }

internal val currentObserver = ThreadLocal<Node?>()

/**
 * States use version/observers, effects use flag/sources, derived values use both.
 */
internal abstract class Node {
    var version = 0L
    val observers = LinkedHashSet<Node>()

    var flag = Flag.DIRTY

    // Dependencies and their versions at the last read
    var sources = LinkedHashMap<Node, Long>()
    var disposed = false
    private var collectedSources: LinkedHashMap<Node, Long>? = null

    open fun refresh() {}

    open fun onMarked() {}

    fun track() {
        currentObserver.get()?.collectedSources?.put(this, version)
    }

    fun needsUpdate(): Boolean {
        if (flag != Flag.CHECK) return flag != Flag.CLEAN

        for ((source, observedVersion) in sources) {
            source.refresh()
            if (source.version != observedVersion) return true
        }

        flag = Flag.CLEAN
        return false
    }

    fun <T> tracked(block: () -> T): T {
        val previousObserver = currentObserver.get()
        val newSources = LinkedHashMap<Node, Long>()
        collectedSources = newSources
        currentObserver.set(this)

        try {
            return block()
        } finally {
            currentObserver.set(previousObserver)
            collectedSources = null

            // Keep dependencies recorded before a failure so later writes can trigger a retry
            for (source in sources.keys) {
                if (source !in newSources) source.observers -= this
            }

            if (disposed) {
                newSources.clear()
            } else {
                for (source in newSources.keys) {
                    source.observers += this
                }
            }
            sources = newSources
        }
    }

    open fun dispose() {
        disposed = true
        sources.keys.forEach { it.observers -= this }
        sources = LinkedHashMap()
        flag = Flag.DIRTY
    }

    fun reachedObservers(): Set<Node> {
        val reached = LinkedHashSet<Node>()

        fun visit(node: Node) {
            for (observer in node.observers) {
                if (reached.add(observer)) visit(observer)
            }
        }

        visit(this)
        return reached
    }
}

internal class StateImpl<T>(initial: T) : Node(), State<T> {
    private var current = initial

    override var value: T
        get() {
            track()
            return current
        }
        set(value) {
            if (value == current) return

            // Validate before mutating state or graph
            val affectedNodes = reachedObservers()
            val affectedFlushers = affectedNodes
                .filterIsInstance<EffectNode>()
                .map { it.flusher }
                .distinct()

            for (flusher in affectedFlushers) {
                flusher.checkWrite()
            }

            current = value
            version++

            for (node in affectedNodes) {
                val requiredFlag = if (node in observers) Flag.DIRTY else Flag.CHECK
                if (node.flag < requiredFlag) node.flag = requiredFlag
                node.onMarked()
            }
        }

    override fun toString(): String = "State($current)"
}

internal class DerivedImpl<T>(private val compute: () -> T) : Node(), Derived<T> {
    private var current: T? = null
    private var hasValue = false

    override val value: T
        get() {
            try {
                refresh()
            } finally {
                track()
            }
            @Suppress("UNCHECKED_CAST")
            return current as T
        }

    override fun refresh() {
        if (!needsUpdate()) return

        flag = Flag.CLEAN
        val nextValue = try {
            tracked(compute)
        } catch (e: Throwable) {
            flag = Flag.DIRTY
            throw e
        }

        if (disposed) flag = Flag.DIRTY
        if (!hasValue || nextValue != current) {
            current = nextValue
            hasValue = true
            version++
        }
    }

    override fun toString(): String = "Derived(${if (hasValue) current else "<not computed>"}, $flag)"
}

internal class EffectNode(
    private val block: EffectScope.() -> Unit,
    val pre: Boolean,
    val id: Long,
    val flusher: Flusher,
) : Node() {
    var queued = false
    private var cleanups: List<() -> Unit> = emptyList()

    override fun onMarked() {
        flusher.enqueue(this)
    }

    fun run() {
        if (disposed) return

        try {
            if (!needsUpdate()) return

            flag = Flag.CLEAN
            runCleanups()

            val scope = EffectScope()
            try {
                tracked { scope.block() }
            } finally {
                cleanups = scope.cleanups
                // Self-disposal already ran old cleanups; run newly registered ones too
                if (disposed) runCleanups()
            }
        } catch (e: Throwable) {
            flag = Flag.DIRTY
            exceptionManager.handleException(e)
        }
    }

    override fun dispose() {
        if (disposed) return

        flusher.remove(this)
        super.dispose()
        runCleanups()
    }

    private fun runCleanups() {
        val pendingCleanups = cleanups
        cleanups = emptyList()

        for (cleanup in pendingCleanups) {
            try {
                cleanup()
            } catch (e: Throwable) {
                exceptionManager.handleException(e)
            }
        }
    }

    override fun toString(): String = "${if (pre) "preEffect" else "effect"}(${block.javaClass.name})"
}

internal class Flusher(private val scheduler: FlushScheduler) {
    private val preEffects = TreeSet<EffectNode>(compareBy { it.id })
    private val effects = TreeSet<EffectNode>(compareBy { it.id })
    private var nextId = 0L
    private var scheduled = false
    private var flushing = false
    val afterFlush = mutableListOf<() -> Unit>()

    fun nextId(): Long = nextId++

    fun checkWrite() {
        scheduler.checkWrite()
    }

    fun enqueue(effect: EffectNode) {
        if (effect.queued || effect.disposed) return

        scheduler.checkWrite()
        effect.queued = true

        val queue = if (effect.pre) preEffects else effects
        queue += effect

        if (!scheduled && !flushing) {
            scheduler.schedule(::flush)
            scheduled = true
        }
    }

    fun remove(effect: EffectNode) {
        preEffects -= effect
        effects -= effect
        effect.queued = false
    }

    fun flush() {
        if (flushing) return

        scheduled = false
        flushing = true

        try {
            var rounds = 0
            while (preEffects.isNotEmpty() || effects.isNotEmpty()) {
                rounds++
                if (rounds > MAX_ROUNDS) {
                    val unsettledEffects = preEffects + effects
                    unsettledEffects.forEach(::remove)
                    throw IllegalStateException(
                        "Effects still dirty after $MAX_ROUNDS flush rounds, they keep writing states they read: $unsettledEffects"
                    )
                }

                runRound(preEffects)
                runRound(effects)
            }

            afterFlush.forEach { it() }
        } finally {
            flushing = false

            // Writes from afterFlush hooks queue effects without scheduling, since the flush was still running
            if (!scheduled && (preEffects.isNotEmpty() || effects.isNotEmpty())) {
                scheduler.schedule(::flush)
                scheduled = true
            }
        }
    }

    private fun runRound(queue: TreeSet<EffectNode>) {
        val batch = queue.toList()
        queue.clear()

        for (effect in batch) {
            effect.queued = false
            effect.run()
        }
    }

    override fun toString(): String = "Flusher(queued=${preEffects.size + effects.size}, flushing=$flushing)"

    private companion object {
        const val MAX_ROUNDS = 100
    }
}
