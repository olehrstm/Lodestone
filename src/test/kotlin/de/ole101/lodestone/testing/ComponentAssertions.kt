package de.ole101.lodestone.testing

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

internal fun Component.plain(): String = PlainTextComponentSerializer.plainText().serialize(this)

/** The content of every text component in this tree, in depth-first order, ignoring spaces and other translatables. */
internal fun Component.texts(): String = ((this as? TextComponent)?.content() ?: "") + children().joinToString("") { it.texts() }

/**
 * Every color explicitly set on this component or one of its children, in depth-first order.
 *
 * Colors are reduced to their plain RGB form, so a named color compares equal to the same value written as hex.
 */
internal fun Component.colors(): List<TextColor> = buildList {
    color()?.let { add(TextColor.color(it.value())) }
    children().forEach { addAll(it.colors()) }
}

/** Every font set on this component or one of its children, in depth-first order. */
internal fun Component.fonts(): List<Key> = listOfNotNull(font()) + children().flatMap { it.fonts() }

/** Every shadow color explicitly set in this component tree. */
internal fun Component.shadows(): List<net.kyori.adventure.text.format.ShadowColor> =
    listOfNotNull(shadowColor()) + children().flatMap { it.shadows() }
