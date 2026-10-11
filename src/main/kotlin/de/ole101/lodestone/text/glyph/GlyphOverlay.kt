package de.ole101.lodestone.text.glyph

import de.ole101.lodestone.LodestoneDsl
import de.ole101.lodestone.text.pixelWidth
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * Composes the glyphs and text added in [block] into one component, placing each at a pixel position.
 * Later items draw on top of earlier ones. Returns an empty component when [block] adds nothing.
 *
 * The result moves the text cursor by 0 in total, so x positions are relative to where the line starts:
 * - In a menu title, that is the title start, 8 pixels right of the menu edge.
 * - In an action bar or boss bar, the client centers the line, so 0 is the screen center and negative x goes left.
 *
 * Items with a y other than 0 are moved by the GUI text shader from the resource pack.
 * Shifted items have these limits:
 * - The shader cannot see inherited colors, so uncolored shifted items are tinted white, the default color of
 *   the action bar and boss bars. Use the overload with [inheritedColor][glyphOverlay] for other places.
 * - Only 8 bits of each tint are kept, so shifted colors are close but not exact.
 * - While the action bar fades out, shifted items snap back to y 0.
 *
 * @see space
 */
public fun glyphOverlay(block: GlyphOverlayBuilder.() -> Unit): Component = glyphOverlay(NamedTextColor.WHITE, block)

/**
 * Like the overload without a color, but tints uncolored shifted items with [inheritedColor] instead of white.
 * Pass the color the line inherits where it is shown, for example `TextColor.color(0x404040)` in a container title.
 */
public fun glyphOverlay(
    inheritedColor: TextColor,
    block: GlyphOverlayBuilder.() -> Unit,
): Component = GlyphOverlayBuilder(inheritedColor).apply(block).build()

@LodestoneDsl
public class GlyphOverlayBuilder internal constructor(
    private val inheritedColor: TextColor,
) {
    private val items = mutableListOf<Item>()

    /**
     * Places [glyph] with its left edge at [x], shifted [y] pixels down.
     *
     * @throws IllegalArgumentException if the glyph has no [Glyph.width].
     */
    public fun glyph(
        glyph: Glyph,
        x: Int,
        y: Int = 0,
    ) {
        place(glyph, x, y, color = null)
    }

    /**
     * Places [glyph] like the overload without a color, but tinted with [color]. [NamedTextColor.WHITE] shows the
     * texture as it is, for example in a menu title, which is dark gray by default.
     *
     * @throws IllegalArgumentException if the glyph has no [Glyph.width].
     */
    public fun glyph(
        glyph: Glyph,
        x: Int,
        y: Int,
        color: TextColor,
    ) {
        place(glyph, x, y, color)
    }

    /**
     * Places [text] so that [x] is its left edge, center or right edge, depending on [align], shifted [y] pixels
     * down. The text is measured with [pixelWidth], so it should use the default font.
     */
    public fun text(
        text: Component,
        x: Int,
        y: Int = 0,
        align: TextAlign = TextAlign.LEFT,
    ) {
        val advance = text.pixelWidth()
        val component = if (y == 0) text else text.withRelativeShadow(y, inheritedColor)
        add(x - align.anchor(advance), advance, component)
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

    /**
     * Unshifted glyphs are plain components. Shifted ones draw the glyph's shadow as a sprite positioned by the
     * GUI text shader, hiding its foreground. A null [color] means uncolored, or [inheritedColor] when shifted.
     */
    private fun place(
        glyph: Glyph,
        x: Int,
        y: Int,
        color: TextColor?,
    ) {
        val width = requireNotNull(glyph.width) { "Glyph needs a width to be placed, but $glyph has none" }
        val component = glyph.asComponent().let {
            if (y == 0) {
                it.color(color)
            } else {
                it.color(SHADER_HIDDEN_COLOR).shadowColor(buildRelativeShadowMarker(y, color ?: inheritedColor))
            }
        }
        add(x, width + 1, component)
    }

    /**
     * Positions text using shadow markers while retaining its font, decorations, and inherited tint.
     */
    private fun Component.withRelativeShadow(
        y: Int,
        fallback: TextColor,
    ): Component {
        val tint = color() ?: fallback
        val marked = color(SHADER_HIDDEN_COLOR)
            .shadowColor(buildRelativeShadowMarker(y, tint))
            .children(children().map { it.withRelativeShadow(y, tint) })

        if (this !is TranslatableComponent || marked !is TranslatableComponent) return marked

        return marked.arguments(
            arguments().map { argument ->
                val value = argument.value()
                if (value is Component) value.withRelativeShadow(y, tint) else argument
            },
        )
    }

    private fun add(
        start: Int,
        advance: Int,
        component: Component,
    ) {
        items += Item(start, advance, component)
    }

    private class Item(
        val start: Int,
        val advance: Int,
        val component: Component,
    )
}

public enum class TextAlign {
    LEFT,
    CENTER,
    RIGHT,
    ;

    internal fun anchor(width: Int): Int = when (this) {
        LEFT -> 0
        CENTER -> width / 2
        RIGHT -> width
    }
}
