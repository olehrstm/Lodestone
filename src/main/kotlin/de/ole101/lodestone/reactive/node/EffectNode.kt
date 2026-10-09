package de.ole101.lodestone.reactive.node

import de.ole101.lodestone.exceptionManager
import de.ole101.lodestone.reactive.flush.Flusher

internal class EffectNode(
    private val block: EffectScope.() -> Unit,
    val pre: Boolean,
    val flusher: Flusher,
) : Node() {
    val id = flusher.nextId()
    private var cleanups: List<() -> Unit> = emptyList()

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
                // Self-disposal already ran old cleanups, run newly registered ones too
                if (disposed) runCleanups()
            }
        } catch (exception: Throwable) {
            flag = Flag.DIRTY
            exceptionManager.handleException(exception)
        }
    }

    override fun dispose() {
        if (disposed) return

        flusher.remove(this)
        super.dispose()
        runCleanups()
    }

    private fun runCleanups() {
        val pending = cleanups
        cleanups = emptyList()
        pending.forEach(::safely)
    }

    override fun toString(): String = "${if (pre) "preEffect" else "effect"}(${block.javaClass.name})"
}

public class EffectScope internal constructor() {
    internal val cleanups = mutableListOf<() -> Unit>()

    /**
     * Runs [block] before reruns and on disposal, in registration order. Failures do not stop other cleanups.
     */
    public fun onCleanup(block: () -> Unit) {
        cleanups += block
    }
}

internal inline fun safely(block: () -> Unit) {
    try {
        block()
    } catch (exception: Throwable) {
        exceptionManager.handleException(exception)
    }
}
