package de.ole101.lodestone.coroutine

import de.ole101.lodestone.event.listen
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import net.minestom.server.entity.Entity
import net.minestom.server.event.entity.EntityDespawnEvent
import net.minestom.server.tag.Tag
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val scopeTag = Tag.Transient<CoroutineScope>("lodestone:coroutine_scope")

/**
 * The coroutine scope of this entity, created on first access and shared by every later call. It resumes
 * coroutines on the entity's own scheduler, during the entity's tick on the thread that ticks it, so they
 * can touch the entity and its surroundings safely. A failing child does not cancel its siblings, and its
 * exception goes to [MinestomExceptionHandler].
 *
 * The scope is cancelled when the entity is removed, for a player also when they disconnect. Coroutines
 * only resume while the entity ticks, so they wait while it is not in an instance and never resume for an
 * entity whose tick does nothing, such as `NoTickingEntity`.
 *
 * @see launch
 * @see tickScope
 */
public val Entity.coroutineScope: CoroutineScope
    get() = getTag(scopeTag) ?: updateAndGetTag(scopeTag) { it ?: newScope() }

/**
 * Launches [block] in this entity's [coroutineScope] and returns its job. [context] and [start] work
 * like in `CoroutineScope.launch`. Exceptions thrown by [block] go to [MinestomExceptionHandler].
 */
public fun Entity.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = coroutineScope.launch(context, start, block)

private fun Entity.newScope(): CoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + scheduler().asCoroutineDispatcher() + MinestomExceptionHandler)

    listen<EntityDespawnEvent>(eventNode()) {
        scope.cancel()
    }

    if (isRemoved) scope.cancel()
    return scope
}
