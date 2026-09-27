package de.ole101.lodestone.text.glyph

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike

/**
 * A single [char] from a resource pack [font], such as `Glyph(Key.key("lodestone:interface"), '\uE000')`.
 *
 * A glyph is [ComponentLike], so it can be appended to other components directly.
 * The component has no color, so it inherits the color of its parent, which tints the glyph texture.
 *
 * [width] is the rendered width in pixels, or null if unknown. Like every glyph, it moves the text cursor by
 * [width] + 1 pixels. Throws [IllegalArgumentException] if [width] is negative.
 */
public class Glyph(
    public val font: Key,
    public val char: Char,
    public val width: Int? = null,
) : ComponentLike {

    init {
        require(width == null || width >= 0) { "Glyph width must not be negative, but was $width" }
    }

    override fun asComponent(): Component = Component.text(char).font(font)

    /** Returns this glyph as a MiniMessage string using the [font] tag. */
    public fun asMini(): String = "<font:${font.asString()}>$char</font>"

    override fun toString(): String = "Glyph(font=${font.asString()}, char=U+%04X, width=$width)".format(char.code)
}
