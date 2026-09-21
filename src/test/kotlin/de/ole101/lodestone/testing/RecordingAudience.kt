package de.ole101.lodestone.testing

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

internal class RecordingAudience : Audience {

    val received: MutableList<Component> = mutableListOf()

    override fun sendMessage(message: Component) {
        this.received += message
    }
}
