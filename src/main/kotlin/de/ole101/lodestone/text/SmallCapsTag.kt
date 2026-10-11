package de.ole101.lodestone.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.tag.Modifying
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

internal object SmallCapsTag : Modifying {
    val RESOLVER: TagResolver = TagResolver.resolver("small") { _, _ -> this }
    private const val SMALL_CAPS = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"

    override fun apply(
        current: Component,
        depth: Int,
    ): Component {
        val converted = if (current is TextComponent) current.content(current.content().toSmallCaps()) else current

        return converted.children(emptyList())
    }

    private fun String.toSmallCaps() = String(CharArray(length) { this[it].toSmallCap() })

    private fun Char.toSmallCap(): Char {
        val lower = lowercaseChar()
        return if (lower in 'a'..'z') SMALL_CAPS[lower - 'a'] else this
    }
}
