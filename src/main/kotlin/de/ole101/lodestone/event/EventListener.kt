package de.ole101.lodestone.event

import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

public abstract class Listener(nodeName: String) {
    public val node: EventNode<Event> = EventNode.all(nodeName)

    public inline fun <reified T : Event> listen(noinline handler: (T) -> Unit) {
        listen(node, handler)
    }

    public fun register() {
        MinecraftServer.getGlobalEventHandler().addChild(node)
    }
}

public inline fun <reified T : Event> listen(nodeName: String, noinline handler: (T) -> Unit): EventNode<Event> {
    return eventNode(nodeName).also {
        listen(it, handler)
    }
}

public inline fun <reified T : Event> listen(node: EventNode<in T> = MinecraftServer.getGlobalEventHandler(), noinline handler: (T) -> Unit) {
    node.addListener(T::class.java, handler)
}

public fun eventNode(nodeName: String): EventNode<Event> {
    val node = EventNode.all(nodeName)

    MinecraftServer.getGlobalEventHandler().addChild(node)
    return node
}
