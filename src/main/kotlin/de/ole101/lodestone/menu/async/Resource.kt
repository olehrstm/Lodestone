package de.ole101.lodestone.menu.async

import de.ole101.lodestone.menu.MenuScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/**
 * Starts [fetch] in this block's coroutine scope and returns a [Resource] that holds its result.
 * [fetch] runs on the tick dispatcher, so move blocking work into `withContext(Dispatchers.IO) { }`.
 */
public fun <T> MenuScope.resource(fetch: suspend () -> T): Resource<T> = Resource(this, fetch).also { it.refetch() }

/** The result of an async fetch in a menu. Reading its properties in an effect or element tracks them. */
public class Resource<T> internal constructor(private val scope: MenuScope, private val fetch: suspend () -> T) {
    private var job: Job? = null

    public var loading: Boolean by scope.state(false)
        private set

    /** The result of the last successful fetch, or null before the first one. Kept while refetching. */
    public var value: T? by scope.state(null)
        private set

    /** The exception of the last fetch, or null if it succeeded or is still running. */
    public var error: Throwable? by scope.state(null)
        private set

    /** Cancels a running fetch and starts a new one. */
    public fun refetch() {
        job?.cancel()
        loading = true
        error = null
        job = scope.launch {
            try {
                value = fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e
            }
            loading = false
        }
    }
}
