package de.ole101.lodestone.text

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike

/**
 * A single [char] from a resource pack [font], such as `Glyph(Key.key("lodestone:interface"), '\uE000')`.
 *
 * A glyph is [ComponentLike], so it can be appended to other components directly.
 * The component has no color, so it inherits the color of its parent, which tints the glyph texture.
 */
public class Glyph(public val font: Key, public val char: Char) : ComponentLike {

    override fun asComponent(): Component = Component.text(char).font(font)

    override fun toString(): String = "Glyph(font=${font.asString()}, char=U+%04X)".format(char.code)
}
