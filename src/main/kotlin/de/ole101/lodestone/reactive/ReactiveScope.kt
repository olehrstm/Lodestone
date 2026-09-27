package de.ole101.lodestone.reactive

/**
 * Owns effects and derived values. Dispose it when its owner goes away.
 *
 * Batches state writes until flush, by default at tick end. Shares its queue with all [child] scopes.
 *
 * Use a scope only on the tick thread (see [FlushScheduler.EndOfTick]).
 */
public class ReactiveScope internal constructor(
    private val flusher: Flusher,
    private val parent: ReactiveScope?,
) {
    private val effects = mutableListOf<EffectNode>()
    private val derivedValues = mutableListOf<DerivedImpl<*>>()
    private val children = mutableListOf<ReactiveScope>()
    private var disposed = false

    public constructor(scheduler: FlushScheduler = FlushScheduler.EndOfTick) : this(Flusher(scheduler), null)

    /** Creates a state with [initial] that needs no disposal and is independent of this scope. */
    public fun <T> state(initial: T): State<T> = StateImpl(initial)

    /**
     * Creates a lazy [Derived] value. [compute] runs on the reading thread; exceptions reach the reader.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun <T> derived(compute: () -> T): Derived<T> {
        checkNotDisposed()
        return DerivedImpl(compute).also { derivedValues += it }
    }

    /**
     * Like [effect], but runs before normal effects in each flush round.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun preEffect(block: EffectScope.() -> Unit) {
        addEffect(block, pre = true)
    }

    /**
     * Runs [block] next flush and after dependencies change. [EffectScope.onCleanup] callbacks run before
     * reruns and on [dispose].
     *
     * Runs in creation order on the flushing thread. Errors go to the exception manager without stopping
     * other effects; the next dependency change retries.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun effect(block: EffectScope.() -> Unit) {
        addEffect(block, pre = false)
    }

    /**
     * Creates a scope that shares this scope's queue and is disposed with it.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun child(): ReactiveScope {
        checkNotDisposed()
        return ReactiveScope(flusher, this).also { children += it }
    }

    /**
     * Runs pending effects of this scope tree now. Does nothing when called from inside an effect.
     *
     * @throws IllegalStateException after 100 unsettled rounds. Pending effects are dropped until dependencies change.
     */
    public fun flush() {
        flusher.flush()
    }

    /**
     * Unsubscribes owned nodes, disposes children, and runs cleanups. Repeated calls do nothing.
     * Cleanup errors go to the exception manager.
     */
    public fun dispose() {
        if (disposed) return
        disposed = true

        children.toList().forEach { it.dispose() }
        effects.forEach { it.dispose() }
        derivedValues.forEach { it.dispose() }

        effects.clear()
        derivedValues.clear()
        parent?.children?.remove(this)
    }

    private fun addEffect(block: EffectScope.() -> Unit, pre: Boolean) {
        checkNotDisposed()

        val effect = EffectNode(block, pre, flusher.nextId(), flusher)
        effects += effect
        flusher.enqueue(effect)
    }

    private fun checkNotDisposed() {
        check(!disposed) { "$this is disposed" }
    }
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
