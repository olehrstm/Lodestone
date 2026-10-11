package de.ole101.lodestone.reactive.node

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Caches a computation and refreshes on read after dependencies change. Equal results (`==`) skip effects.
 *
 * Exceptions reach the reader. The next read retries. Supports `val line by scope.derived { ... }`.
 *
 * @see de.ole101.lodestone.reactive.ReactiveScope.derived
 */
public sealed interface Derived<T> : ReadOnlyProperty<Any?, T> {
    public val value: T

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = value
}

internal class DerivedImpl<T>(
    private val compute: () -> T,
) : Node(),
    Derived<T> {
    private var current: T? = null
    private var hasValue = false

    override val value: T
        get() {
            try {
                refresh()
            } finally {
                track()
            }
            return current as T
        }

    override fun refresh() {
        if (!needsUpdate()) return

        flag = Flag.CLEAN
        val nextValue = try {
            tracked(compute)
        } catch (exception: Throwable) {
            flag = Flag.DIRTY
            throw exception
        }

        if (disposed) flag = Flag.DIRTY
        if (!hasValue || nextValue != current) {
            current = nextValue
            hasValue = true
            version++
        }
    }
}
