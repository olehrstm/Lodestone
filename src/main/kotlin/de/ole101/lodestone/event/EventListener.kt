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

public inline fun <reified T : Event> listen(nodeName: String, noinline handler: (T) -> Unit): EventNode<Event> {
    return eventNode(nodeName).also {
        listen(it, handler)
    }
}

public inline fun <reified T : Event> listen(node: EventNode<in T> = globalEventHandler, noinline handler: (T) -> Unit) {
    node.addListener(T::class.java, handler)
}

public fun eventNode(nodeName: String): EventNode<Event> {
    val node = EventNode.all(nodeName)

    globalEventHandler.addChild(node)
    return node
}
