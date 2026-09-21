package de.ole101.lodestone.testing

import net.kyori.adventure.identity.Identity
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender
import net.minestom.server.tag.TagHandler

internal open class TestSender : CommandSender {

    val received: MutableList<Component> = mutableListOf()

    private val tagHandler = TagHandler.newHandler()
    private val identity = Identity.nil()
    private val pointers = Pointers.builder().withStatic(Identity.UUID, this.identity.uuid()).build()

    override fun sendMessage(message: Component) {
        this.received += message
    }

    override fun tagHandler(): TagHandler = this.tagHandler

    override fun identity(): Identity = this.identity

    override fun pointers(): Pointers = this.pointers
}

internal class AdminSender : TestSender()
