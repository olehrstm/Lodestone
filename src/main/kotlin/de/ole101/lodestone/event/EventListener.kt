package de.ole101.lodestone.event

import de.ole101.lodestone.globalEventHandler
import net.minestom.server.event.Event
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode

/**
 * Creates an event node called [nodeName], adds it to the [globalEventHandler] and adds [handler]
 * for events of type [T] to it. Returns the new node, so the listener can be removed later.
 *
 * @see eventNode
 */
public inline fun <reified T : Event> listen(
    nodeName: String,
    noinline handler: (T) -> Unit,
): EventNode<Event> = eventNode(nodeName).also {
    listen(it, handler)
}

/**
 * Adds [handler] for events of type [T] to [node], which defaults to the [globalEventHandler].
 */
public inline fun <reified T : Event> listen(
    node: EventNode<in T> = globalEventHandler,
    noinline handler: (T) -> Unit,
) {
    node.addListener(T::class.java, handler)
}

/**
 * Creates an event node called [nodeName] and adds it to the [globalEventHandler].
 */
public fun eventNode(nodeName: String): EventNode<Event> {
    val node = EventNode.all(nodeName)

    globalEventHandler.addChild(node)
    return node
}

/**
 * Adds a listener for events of type [T] to [node] that only sees events passing [filter] and is removed
 * after the first one it handles. Events rejected by [filter] do not count. Returns the listener, so it can
 * be removed from [node] earlier.
 */
public inline fun <reified T : Event> listenOnce(
    node: EventNode<in T>,
    noinline filter: (T) -> Boolean = { true },
    noinline handler: (T) -> Unit,
): EventListener<T> = EventListener
    .builder(T::class.java)
    .filter(filter)
    .expireCount(1)
    .handler(handler)
    .build()
    .also { node.addListener(it) }

/**
 * Base class for a group of listeners that share one event node called [nodeName].
 *
 * Add listeners with [listen], typically in an `init` block, then call [register] to attach the node to the
 * [globalEventHandler]. Listeners only receive events after [register] has been called.
 */
public abstract class Listener(
    nodeName: String,
) {
    public val node: EventNode<Event> = EventNode.all(nodeName)

    public inline fun <reified T : Event> listen(noinline handler: (T) -> Unit) {
        listen(node, handler)
    }

    public fun register() {
        globalEventHandler.addChild(node)
    }
}
