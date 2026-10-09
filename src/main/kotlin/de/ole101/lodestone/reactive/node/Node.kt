package de.ole101.lodestone.reactive.node

internal enum class Flag { CLEAN, CHECK, DIRTY }

// Reads of the running effect or derived value, null when untracked. Per thread because tick threads run in parallel
private val currentSources = ThreadLocal<LinkedHashMap<Node, Long>?>()

private inline fun <T> withSources(
    sources: LinkedHashMap<Node, Long>?,
    block: () -> T,
): T {
    val previous = currentSources.get()
    currentSources.set(sources)
    try {
        return block()
    } finally {
        currentSources.set(previous)
    }
}

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

    open fun refresh() {}

    fun track() {
        currentSources.get()?.put(this, version)
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
        val newSources = LinkedHashMap<Node, Long>()
        try {
            return withSources(newSources, block)
        } finally {
            // Keep dependencies recorded before a failure so later writes can trigger a retry
            for (source in sources.keys - newSources.keys) source.observers -= this
            if (disposed) newSources.clear()
            for (source in newSources.keys) source.observers += this
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

/**
 * Runs [block] without tracking its reads for the current observer.
 */
public fun <T> untracked(block: () -> T): T = withSources(null, block)
