package de.ole101.lodestone.text.glyph

import de.ole101.lodestone.text.pixelWidth
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * Composes the glyphs and text added in [block] into one component, placing each at a pixel position.
 * Later items draw on top of earlier ones.
 *
 * The result moves the text cursor by 0 in total, so positions are relative to where the line starts.
 * In a menu title that is the title start, 8 pixels right of the menu edge. In an action bar or boss bar
 * the client centers the line, so 0 is the screen center and negative x goes left.
 *
 * Items with a y other than 0 are drawn in fonts from [shiftedFonts]. With the default [ShiftedFonts.None]
 * this throws [IllegalStateException]. It can be called from any thread, because it only builds components.
 * Returns an empty component when [block] adds nothing.
 *
 * @see space
 */
public fun glyphOverlay(shiftedFonts: ShiftedFonts = ShiftedFonts.None, block: GlyphOverlayBuilder.() -> Unit): Component = GlyphOverlayBuilder(shiftedFonts).apply(block).build()

@DslMarker
public annotation class GlyphOverlayDsl

@GlyphOverlayDsl
public class GlyphOverlayBuilder internal constructor(private val shiftedFonts: ShiftedFonts) {
    private val items = mutableListOf<Item>()

    /**
     * Places [glyph] with its left edge at [x], shifted [y] pixels down.
     * Throws [IllegalArgumentException] if the glyph has no [Glyph.width].
     */
    public fun glyph(glyph: Glyph, x: Int, y: Int = 0) {
        add(glyph, glyph.asComponent(), x, y)
    }

    /**
     * Places [glyph] like the overload without a color, but tinted with [color]. [NamedTextColor.WHITE] shows the
     * texture as it is, for example in a menu title, which is dark gray by default.
     * Throws [IllegalArgumentException] if the glyph has no [Glyph.width].
     */
    public fun glyph(glyph: Glyph, x: Int, y: Int, color: TextColor) {
        add(glyph, glyph.asComponent().color(color), x, y)
    }

    private fun add(glyph: Glyph, component: Component, x: Int, y: Int) {
        val width = requireNotNull(glyph.width) { "Glyph needs a width to be placed, but $glyph has none" }
        add(x, width + 1, component, y)
    }

    /**
     * Places [text] so that [x] is its left edge, center or right edge, depending on [align], shifted [y] pixels
     * down. The text is measured with [pixelWidth], so it should use the default font.
     */
    public fun text(text: Component, x: Int, y: Int = 0, align: TextAlign = TextAlign.LEFT) {
        val advance = text.pixelWidth()
        add(x - align.anchor(advance), advance, text, y)
    }

    private fun add(start: Int, advance: Int, component: Component, y: Int) {
        val shifted = if (y == 0) component else component.shiftedDown(y, isRoot = true)
        items += Item(start, advance, shifted)
    }

    // The root gets a shifted font even without its own font, descendants only when they set one,
    // everything else inherits it.
    private fun Component.shiftedDown(y: Int, isRoot: Boolean): Component {
        val own = font()
        val refonted = if (own != null || isRoot) font(shiftedFonts.font(own ?: DEFAULT_FONT, y)) else this
        return refonted.children(children().map { it.shiftedDown(y, isRoot = false) })
    }

    internal fun build(): Component {
        if (items.isEmpty()) return Component.empty()

        val line = Component.text()
        var cursor = 0
        for (item in items) {
            line.append(space(item.start - cursor), item.component)
            cursor = item.start + item.advance
        }
        return line.append(space(-cursor)).build()
    }

    private class Item(val start: Int, val advance: Int, val component: Component)

    private companion object {
        val DEFAULT_FONT: Key = Key.key("minecraft:default")
    }
}

public enum class TextAlign {
    LEFT,
    CENTER,
    RIGHT
}

internal fun TextAlign.anchor(width: Int): Int = when (this) {
    TextAlign.LEFT -> 0
    TextAlign.CENTER -> width / 2
    TextAlign.RIGHT -> width
}
