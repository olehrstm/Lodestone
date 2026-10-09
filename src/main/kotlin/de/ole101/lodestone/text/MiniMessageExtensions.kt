package de.ole101.lodestone.text

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

/**
 * Returns this string as a MiniMessage component.
 */
public fun String.mini(): Component = MiniMessageProvider.parse(this)

/**
 * Returns this string as a plain text component.
 */
public fun String.component(): Component = Component.text(this)

public fun Audience.sendMini(message: String) {
    this.sendMessage(message.mini())
}

public fun String.sendMini(vararg audiences: Audience) {
    val component = this.mini()
    audiences.forEach { it.sendMessage(component) }
}

public fun Component.send(vararg audiences: Audience) {
    audiences.forEach { it.sendMessage(this) }
}
