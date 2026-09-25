package de.ole101.lodestone.text

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

/** Parses this string as MiniMessage with [MiniMessageProvider]. */
public fun String.mini(): Component {
    return MiniMessageProvider.parse(this)
}

/** Returns this string as a plain text component. MiniMessage tags are not parsed. */
public fun String.component(): Component {
    return Component.text(this)
}

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
