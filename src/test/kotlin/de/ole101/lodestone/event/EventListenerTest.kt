package de.ole101.lodestone.event

import de.ole101.lodestone.globalEventHandler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

private class PingEvent : Event
private class PongEvent : Event

class EventListenerTest : FunSpec({

    MinecraftServer.init()

    test("listen only receives events of the reified type") {
        val node = EventNode.all("typed")
        val received = mutableListOf<Event>()
        listen<PingEvent>(node) { received += it }

        val ping = PingEvent()
        node.call(ping)
        node.call(PongEvent())

        received shouldContainExactly listOf(ping)
    }

    test("listen by name attaches to the global handler and returns a removable node") {
        val received = mutableListOf<Event>()
        val node = listen<PingEvent>("named") { received += it }

        val first = PingEvent()
        globalEventHandler.call(first)
        globalEventHandler.removeChild(node)
        globalEventHandler.call(PingEvent())

        received shouldContainExactly listOf(first)
    }

    test("Listener only receives events once registered") {
        val received = mutableListOf<Event>()
        val listener = object : Listener("listener") {
            init {
                listen<PingEvent> { received += it }
            }
        }

        globalEventHandler.call(PingEvent())
        received.shouldBeEmpty()

        listener.register()
        val event = PingEvent()
        globalEventHandler.call(event)
        globalEventHandler.removeChild(listener.node)

        received shouldContainExactly listOf(event)
    }
})
