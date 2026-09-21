package de.ole101.lodestone.testing

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

internal fun Component.plain(): String = PlainTextComponentSerializer.plainText().serialize(this)

/**
 * Every color explicitly set on this component or one of its children, in depth-first order.
 *
 * Colors are reduced to their plain RGB form, so a named color compares equal to the same value written as hex.
 */
internal fun Component.colors(): List<TextColor> = buildList {
    color()?.let { add(TextColor.color(it.value())) }
    children().forEach { addAll(it.colors()) }
}
