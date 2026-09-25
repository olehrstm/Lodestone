package de.ole101.lodestone.event

import de.ole101.lodestone.globalEventHandler
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

public abstract class Listener(nodeName: String) {
    public val node: EventNode<Event> = EventNode.all(nodeName)

    public inline fun <reified T : Event> listen(noinline handler: (T) -> Unit) {
        listen(node, handler)
    }

    public fun register() {
        globalEventHandler.addChild(node)
    }
}

/**
 * Creates an event node called [nodeName], adds it to the [globalEventHandler] and adds [handler]
 * for events of type [T] to it. Returns the new node, so the listener can be removed later.
 *
 * @see eventNode
 */
public inline fun <reified T : Event> listen(nodeName: String, noinline handler: (T) -> Unit): EventNode<Event> {
    return eventNode(nodeName).also {
        listen(it, handler)
    }
}

/**
 * Adds [handler] for events of type [T] to [node], which defaults to the [globalEventHandler].
 */
public inline fun <reified T : Event> listen(node: EventNode<in T> = globalEventHandler, noinline handler: (T) -> Unit) {
    node.addListener(T::class.java, handler)
}

/** Creates an event node called [nodeName] and adds it to the [globalEventHandler]. */
public fun eventNode(nodeName: String): EventNode<Event> {
    val node = EventNode.all(nodeName)

    globalEventHandler.addChild(node)
    return node
}
