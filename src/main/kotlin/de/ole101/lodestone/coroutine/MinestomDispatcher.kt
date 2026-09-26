package de.ole101.lodestone.coroutine

import de.ole101.lodestone.exceptionManager
import de.ole101.lodestone.schedulerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Resumes coroutines at the start of the next server tick, on the tick scheduler thread, where it is
 * safe to touch players, inventories and other server state. A resume can wait up to one tick (50 ms).
 *
 * Do not block on this dispatcher: it stalls the whole tick. Move blocking calls into
 * `withContext(Dispatchers.IO) { }`. `delay` waits off-thread and resumes on the first tick after it ends.
 *
 * @see tickScope
 */
public val Minestom: CoroutineDispatcher by lazy { schedulerManager.asCoroutineDispatcher() }

public val MinestomExceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e -> exceptionManager.handleException(e) }

/**
 * Creates a new scope that runs its coroutines on [Minestom]. A failing child does not cancel
 * its siblings, and its exception goes to [MinestomExceptionHandler].
 *
 * The owner of the scope (a menu, a command, a player session) must call `cancel()` when it goes away,
 * or its coroutines keep running.
 */
public fun tickScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Minestom + MinestomExceptionHandler)
