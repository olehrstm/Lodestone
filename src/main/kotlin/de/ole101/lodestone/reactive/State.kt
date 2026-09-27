package de.ole101.lodestone.reactive

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Reading [value] in a derived computation or effect tracks it. Changed assignments queue dependent effects.
 *
 * Reassign collections to notify readers; in-place mutations are not tracked.
 *
 * Write only on the tick thread (see [FlushScheduler.EndOfTick]). Supports `var coins by state(0)`.
 *
 * @see state
 */
public sealed interface State<T> : ReadWriteProperty<Any?, T> {
    public var value: T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

/**
 * Caches a computation and refreshes on read after dependencies change. Equal results (`==`) skip effects.
 *
 * Exceptions reach the reader; the next read retries. Supports `val line by scope.derived { ... }`.
 *
 * @see ReactiveScope.derived
 */
public sealed interface Derived<T> : ReadOnlyProperty<Any?, T> {
    public val value: T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

public fun <T> state(initial: T): State<T> = StateImpl(initial)

/** Runs [block] without tracking its reads for the current observer. */
public fun <T> untrack(block: () -> T): T {
    val previous = currentObserver.get()
    currentObserver.set(null)
    try {
        return block()
    } finally {
        currentObserver.set(previous)
    }
}
