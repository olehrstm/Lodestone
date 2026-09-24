package de.ole101.lodestone.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.tag.Modifying
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

internal class SmallCapsTag : Modifying {

    override fun apply(current: Component, depth: Int): Component {
        if (current !is TextComponent) return current.children(emptyList())

        return current.content(current.content().toSmallCaps()).children(emptyList())
    }

    private fun String.toSmallCaps(): String = buildString(this.length) {
        this@toSmallCaps.forEach { char ->
            append(CHARACTER_TRANSLATIONS[char.lowercaseChar()] ?: char)
        }
    }

    public companion object {
        public val RESOLVER: TagResolver = TagResolver.resolver("small") { _, _ -> SmallCapsTag() }

        private val CHARACTER_TRANSLATIONS = mapOf(
            'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ',
            'f' to 'ғ', 'g' to 'ɢ', 'h' to 'ʜ', 'i' to 'ɪ', 'j' to 'ᴊ',
            'k' to 'ᴋ', 'l' to 'ʟ', 'm' to 'ᴍ', 'n' to 'ɴ', 'o' to 'ᴏ',
            'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ', 's' to 's', 't' to 'ᴛ',
            'u' to 'ᴜ', 'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x', 'y' to 'ʏ',
            'z' to 'ᴢ'
        )
    }
}
