package de.ole101.lodestone.reactive.flush

import de.ole101.lodestone.reactive.node.EffectNode
import java.util.TreeSet

internal class Flusher(
    private val scheduler: FlushScheduler,
) {
    // Pre-effects first, then by creation order
    private val queue = TreeSet<EffectNode>(compareBy({ !it.pre }, { it.id }))
    private var nextId = 0L
    private var scheduled = false
    private var flushing = false
    private val afterFlush = mutableListOf<() -> Unit>()

    fun nextId() = nextId++

    fun checkWrite() = scheduler.checkWrite()

    fun addAfterFlush(block: () -> Unit): () -> Unit {
        afterFlush += block
        return { afterFlush -= block }
    }

    fun enqueue(effect: EffectNode) {
        if (effect.disposed || effect in queue) return

        checkWrite()
        queue += effect

        if (!flushing) scheduleFlush()
    }

    fun remove(effect: EffectNode) {
        queue -= effect
    }

    fun flush() {
        if (flushing) return

        scheduled = false
        flushing = true

        try {
            var runs = 0
            while (queue.isNotEmpty()) {
                if (++runs > MAX_RUNS) {
                    val unsettledEffects = queue.toList()
                    queue.clear()
                    error("Effects still dirty after $MAX_RUNS runs in one flush, they keep writing states they read: $unsettledEffects")
                }

                queue.pollFirst()?.run()
            }

            afterFlush.toList().forEach { it() }
        } finally {
            flushing = false

            // Writes from afterFlush hooks queue effects without scheduling, since the flush was still running
            if (queue.isNotEmpty()) scheduleFlush()
        }
    }

    private fun scheduleFlush() {
        if (scheduled) return

        scheduler.schedule(::flush)
        scheduled = true
    }

    private companion object {
        const val MAX_RUNS = 10_000
    }
}
