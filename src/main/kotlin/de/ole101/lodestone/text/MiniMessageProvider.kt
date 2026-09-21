package de.ole101.lodestone.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

public object MiniMessageProvider {

    @Volatile
    public var instance: MiniMessage = MiniMessage.builder()
        .editTags { it.resolver(ColorTag()) }
        .build()

    public fun parse(message: String): Component {
        return this.instance.deserialize(message)
    }
}
