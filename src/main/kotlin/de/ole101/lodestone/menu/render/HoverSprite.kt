package de.ole101.lodestone.menu.render

import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.Position
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.SHADER_HIDDEN_COLOR
import de.ole101.lodestone.text.glyph.TextAlign
import de.ole101.lodestone.text.glyph.buildHoverShadow
import de.ole101.lodestone.text.glyph.space
import de.ole101.lodestone.text.pixelWidth
import net.kyori.adventure.text.Component

/**
 * Returns the start of a tooltip that shows [glyph], a hover sprite with shader data rows,
 * centered over the cells [at] while the item is hovered. [tooltip] is the rest of the tooltip's first line.
 *
 * It needs text shaders in the resource pack that read this data. The glyph itself is drawn in
 * [SHADER_HIDDEN_COLOR], which the shader hides, and its shadow carries the data from [buildHoverShadow].
 */
internal fun RenderScope.hoverSprite(
    glyph: Glyph,
    at: Position,
    tooltip: Component?,
): Component {
    val (x, y) = glyphPosition(glyph, at, TextAlign.CENTER, glyph.centeredVertically())
    // The client centers the GUI on the screen, so its center is the screen center
    val dx = x - MenuMetrics.GUI_WIDTH / 2
    val dy = y - session.type.gui.height / 2
    val shadow = buildHoverShadow(dx, dy, tooltip?.pixelWidth() ?: 0, HOVER_LINES)

    // Cancels the glyph's advance, so the tooltip text after it starts where the line starts
    val advance = requireNotNull(glyph.width) + 1
    return Component.textOfChildren(
        glyph.asComponent().color(SHADER_HIDDEN_COLOR).shadowColor(shadow),
        space(-advance),
    )
}

private const val HOVER_LINES = 1
