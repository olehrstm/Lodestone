package de.ole101.lodestone.menu.render

import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.Position
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.SHADER_HIDDEN_COLOR
import de.ole101.lodestone.text.glyph.TextAlign
import de.ole101.lodestone.text.glyph.space
import de.ole101.lodestone.text.pixelWidth
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor

/**
 * Returns the start of a tooltip that shows [glyph], a hover sprite with shader data rows,
 * centered over the cells [at] while the item is hovered. [tooltip] is the rest of the tooltip's first line.
 *
 * It needs text shaders in the resource pack that read this data. The glyph itself is drawn in
 * [SHADER_HIDDEN_COLOR], which the shader hides. Its shadow carries the data. Red and green are the sprite's top-left
 * corner relative to the screen center plus 128, blue and the top 3 bits of alpha are the tooltip width, and the low
 * 5 bits of alpha are the number of tooltip lines. The shader draws the shadow at the target and cuts out the part
 * behind the tooltip.
 */
internal fun RenderScope.hoverSprite(
    glyph: Glyph,
    at: Position,
    tooltip: Component?,
): Component {
    val (x, y) = glyphPosition(glyph, at, TextAlign.CENTER, glyph.centeredVertically())
    // The client centers the GUI on the screen, so its center is the screen center
    val dx = x - MenuMetrics.GUI_WIDTH / 2 + HOVER_COORDINATE_OFFSET
    val dy = y - session.type.gui.height / 2 + HOVER_COORDINATE_OFFSET
    require(dx in 0..HOVER_COORDINATE_MAX && dy in 0..HOVER_COORDINATE_MAX) {
        "Hover sprites must start within 128 pixels of the GUI center, but $glyph is at $x, $y"
    }

    val width = tooltip?.pixelWidth()?.coerceIn(0, 2047) ?: 0
    val shadow = ShadowColor.shadowColor(dx, dy, width and 0xFF, (width shr 8 shl 5) or HOVER_LINES)

    // Cancels the glyph's advance, so the tooltip text after it starts where the line starts
    val advance = requireNotNull(glyph.width) + 1
    return Component.textOfChildren(
        glyph.asComponent().color(SHADER_HIDDEN_COLOR).shadowColor(shadow),
        space(-advance),
    )
}

private const val HOVER_COORDINATE_OFFSET = 128
private const val HOVER_COORDINATE_MAX = 255
private const val HOVER_LINES = 1
