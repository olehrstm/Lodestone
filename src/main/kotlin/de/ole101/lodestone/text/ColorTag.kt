package de.ole101.lodestone.text

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

internal class ColorTag : TagResolver {

    override fun resolve(
        name: String,
        arguments: ArgumentQueue,
        ctx: Context
    ): Tag? {
        val color = this.colorFor(name) ?: return null

        return Tag.styling(color)
    }

    override fun has(name: String): Boolean {
        return this.colorFor(name) != null
    }

    private fun colorFor(name: String): TextColor? {
        if (!name.startsWith("!")) return null

        return Colors.named(name.substring(1))
    }
}
