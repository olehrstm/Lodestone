package de.ole101.lodestone.reactive.node

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Reading [value] in a derived computation or effect tracks it. Changed assignments queue dependent effects.
 *
 * Reassign collections to notify readers. In-place mutations are not tracked.
 *
 * Write only on the tick thread (see [de.ole101.lodestone.reactive.flush.FlushScheduler.EndOfTick]). Supports `var coins by state(0)`.
 *
 * @see state
 */
public sealed interface State<T> : ReadWriteProperty<Any?, T> {
    public var value: T

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = value

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T,
    ) {
        this.value = value
    }
}

/**
 * Creates a state with [initial]. It needs no disposal and is independent of any scope.
 */
public fun <T> state(initial: T): State<T> = StateImpl(initial)

internal class StateImpl<T>(
    initial: T,
) : Node(),
    State<T> {
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
            affectedNodes
                .filterIsInstance<EffectNode>()
                .map { it.flusher }
                .distinct()
                .forEach { it.checkWrite() }

            current = value
            version++

            for (node in affectedNodes) {
                node.flag = maxOf(node.flag, if (node in observers) Flag.DIRTY else Flag.CHECK)
                if (node is EffectNode) node.flusher.enqueue(node)
            }
        }
}
