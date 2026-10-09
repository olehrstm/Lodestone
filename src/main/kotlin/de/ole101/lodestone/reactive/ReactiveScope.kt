package de.ole101.lodestone.reactive

import de.ole101.lodestone.reactive.flush.FlushScheduler
import de.ole101.lodestone.reactive.flush.Flusher
import de.ole101.lodestone.reactive.node.Derived
import de.ole101.lodestone.reactive.node.DerivedImpl
import de.ole101.lodestone.reactive.node.EffectNode
import de.ole101.lodestone.reactive.node.EffectScope
import de.ole101.lodestone.reactive.node.Node
import de.ole101.lodestone.reactive.node.State
import de.ole101.lodestone.reactive.node.StateImpl
import de.ole101.lodestone.reactive.node.safely

/**
 * Owns effects and derived values. Dispose it when its owner goes away.
 *
 * Batches state writes until flush, by default at tick end. Shares its queue with all [child] scopes.
 *
 * Use a scope only on the tick thread (see [de.ole101.lodestone.reactive.flush.FlushScheduler.EndOfTick]).
 */
public class ReactiveScope internal constructor(
    private val flusher: Flusher,
    private val parent: ReactiveScope?,
) {
    private val owned = mutableListOf<Node>()
    private val children = mutableListOf<ReactiveScope>()
    private val disposeHooks = mutableListOf<() -> Unit>()
    private var disposed = false

    public constructor(scheduler: FlushScheduler = FlushScheduler.EndOfTick) : this(Flusher(scheduler), null)

    /**
     * Creates a state with [initial] that needs no disposal and is independent of this scope.
     */
    public fun <T> state(initial: T): State<T> = StateImpl(initial)

    /**
     * Creates a lazy [de.ole101.lodestone.reactive.node.Derived] value. [compute] runs on the reading thread. Exceptions reach the reader.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun <T> derived(compute: () -> T): Derived<T> {
        checkNotDisposed()

        return DerivedImpl(compute).also { owned += it }
    }

    /**
     * Like [effect], but runs before any queued normal effect.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun preEffect(block: EffectScope.() -> Unit) {
        addEffect(pre = true, block)
    }

    /**
     * Runs [block] next flush and after dependencies change. [EffectScope.onCleanup] callbacks run before
     * reruns and on [dispose].
     *
     * Runs in creation order on the flushing thread. Errors go to the exception manager without stopping
     * other effects. The next dependency change retries.
     *
     * @throws IllegalStateException if this scope is disposed.
     */
    public fun effect(block: EffectScope.() -> Unit) {
        addEffect(pre = false, block)
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
     * @throws IllegalStateException after 10,000 effect runs in one flush. Pending effects are dropped until dependencies change.
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

        children.toList().forEach(ReactiveScope::dispose)
        owned.forEach(Node::dispose)
        owned.clear()
        parent?.children?.remove(this)

        disposeHooks.forEach(::safely)
        disposeHooks.clear()
    }

    /**
     * Runs [block] once when this scope is disposed, after its effects and children.
     */
    internal fun onDispose(block: () -> Unit) {
        checkNotDisposed()

        disposeHooks += block
    }

    /**
     * Runs [block] at the end of every flush of this scope tree that settles without throwing.
     */
    internal fun afterFlush(block: () -> Unit) {
        checkNotDisposed()

        onDispose(flusher.addAfterFlush(block))
    }

    private fun addEffect(
        pre: Boolean,
        block: EffectScope.() -> Unit,
    ) {
        checkNotDisposed()

        val effect = EffectNode(block, pre, flusher)
        flusher.enqueue(effect)
        owned += effect
    }

    private fun checkNotDisposed() {
        check(!disposed) { "This ReactiveScope is disposed" }
    }
}
