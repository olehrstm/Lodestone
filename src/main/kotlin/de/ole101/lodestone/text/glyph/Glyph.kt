package de.ole101.lodestone.text.glyph

import de.ole101.lodestone.text.MiniMessageProvider
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
 * [width] + 1 pixels. [height] is the rendered height in pixels, or null if unknown. It is only needed to align
 * the glyph vertically. [ascent] is the `ascent` of the glyph's bitmap provider in the resource pack. Menus place the
 * top of a glyph where default-font text would start, which needs the default [DEFAULT_ASCENT]. Glyphs shorter than
 * that must use a smaller ascent, and menus move them up by the difference.
 * Throws [IllegalArgumentException] if [width] or [height] is negative, or [ascent] is above [height].
 */
public class Glyph @JvmOverloads constructor(
    public val font: Key,
    public val char: Char,
    public val width: Int? = null,
    public val height: Int? = null,
    public val ascent: Int = DEFAULT_ASCENT,
) : ComponentLike {

    init {
        require(width == null || width >= 0) { "Glyph width must not be negative, but was $width" }
        require(height == null || height >= 0) { "Glyph height must not be negative, but was $height" }
        require(height == null || ascent <= height) { "Glyph ascent must not be above its height $height, but was $ascent" }
    }

    override fun asComponent(): Component = Component.text(char).font(font)

    /** Returns this glyph as a MiniMessage string using the [font] tag. */
    public fun asMini(): String = "<font:${font.asString()}>${MiniMessageProvider.escapeTags(char.toString())}</font>"

    override fun toString(): String = "Glyph(font=${font.asString()}, char=U+%04X, width=$width, height=$height, ascent=$ascent)".format(char.code)

    public companion object {
        /** The ascent of the default font, where the top of a glyph lines up with default-font text. */
        public const val DEFAULT_ASCENT: Int = 7
    }
}
