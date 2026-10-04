package de.ole101.lodestone.menu.element

import de.ole101.lodestone.item.item
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.*
import de.ole101.lodestone.menu.render.RenderScope
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
import de.ole101.lodestone.text.glyph.space
import de.ole101.lodestone.text.pixelWidth
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * Adds the slot at [index], configured by [block]. The block runs again whenever a state it read changes, for
 * example inside `item { }`. Throws [IllegalArgumentException] if [index] is outside the menu.
 */
public fun MenuScope.slot(index: Int, block: SlotScope.() -> Unit) {
    session.type.checkSlot(index)
    element { slot(index, block) }
}

public fun MenuScope.slot(column: Int, row: Int, block: SlotScope.() -> Unit) {
    slot(session.type.slotIndex(column, row), block)
}

/**
 * Draws [glyph] in the title at [at], aligned with [align] and, inside a cell, [vAlign], tinted with [color]. Throws
 * [IllegalArgumentException] if the glyph has no [Glyph.width], or no [Glyph.height] when [vAlign] is not [VAlign.TOP].
 *
 * @see de.ole101.lodestone.menu.render.RenderScope.glyph
 */
public fun MenuScope.overlay(
    glyph: Glyph,
    at: Position,
    align: TextAlign = TextAlign.LEFT,
    vAlign: VAlign = VAlign.TOP,
    color: TextColor = NamedTextColor.WHITE,
) {
    requireNotNull(glyph.width) { "Glyph needs a width to be drawn in a menu, but $glyph has none" }
    require(vAlign == VAlign.TOP || glyph.height != null) { "Glyph needs a height to be aligned $vAlign, but $glyph has none" }
    element { glyph(glyph, at, align, vAlign, color) }
}

/**
 * Draws a full-width sprite behind rows of the menu. [glyph] returns the sprite for each row, counted from 0, or null
 * to leave the row as it is. Each sprite's top-left corner is at the left edge of the menu and the top of the row's
 * slots, so it fits sprites as wide as [MenuMetrics.GUI_WIDTH]. Call it before other overlays to draw it below them.
 * Throws [IllegalArgumentException] if a glyph has no [Glyph.width].
 */
public fun MenuScope.rowOverlays(glyph: (row: Int) -> Glyph?) {
    for (row in 0 until session.type.rows) {
        val rowGlyph = glyph(row) ?: continue
        overlay(rowGlyph, px(0, session.type.slotY(row)))
    }
}

/**
 * Draws the default-font text that [text] returns in the title, and redraws it when a state it read changes.
 *
 * @see de.ole101.lodestone.menu.render.RenderScope.text
 */
public fun MenuScope.text(at: Position, align: TextAlign = TextAlign.LEFT, vAlign: VAlign = VAlign.TOP, text: () -> Component) {
    element { text(at, align, vAlign, text) }
}

/** Draws [glyph] centered over [columns] of [row], also vertically when the glyph has a height. */
internal fun RenderScope.spanGlyph(glyph: Glyph, columns: IntRange, row: Int) {
    glyph(glyph, cell(columns, row), TextAlign.CENTER, glyph.centeredVertically())
}

/**
 * Returns the start of a tooltip that shows [glyph], a hover sprite with shader data rows,
 * centered over [columns] of [row] while the item is hovered. [tooltip] is the rest of the tooltip's first line.
 *
 * It needs text shaders in your resource pack that read this data. The glyph itself is drawn in [HOVER_HIDDEN_COLOR], which
 * the shader hides. Its shadow carries the data. Red and green are the sprite's top-left corner relative to the
 * screen center plus 128, blue and the top 3 bits of alpha are the tooltip width, and the low 5 bits of alpha are the number
 * of tooltip lines. The shader draws the shadow at the target and cuts out the part behind the tooltip.
 */
internal fun RenderScope.hoverSprite(glyph: Glyph, columns: IntRange, row: Int, tooltip: Component?): Component {
    val (x, y) = glyphPosition(glyph, cell(columns, row), TextAlign.CENTER, glyph.centeredVertically())
    // The client centers the GUI on the screen, so its center is the screen center
    val dx = x - MenuMetrics.GUI_WIDTH / 2 + 128
    val dy = y - session.type.guiHeight / 2 + 128
    require(dx in 0..255 && dy in 0..255) { "Hover sprites must start within 128 pixels of the GUI center, but $glyph is at $x, $y" }

    val width = (tooltip?.pixelWidth() ?: 0).coerceIn(0, 2047)
    val lines = 1
    val shadow = ShadowColor.shadowColor(dx, dy, width and 0xFF, ((width shr 8) shl 5) or lines)

    // Cancels the glyph's advance, so the tooltip text after it starts where the line starts
    val advance = requireNotNull(glyph.width) + 1
    return Component.text()
        .append(glyph.asComponent().color(HOVER_HIDDEN_COLOR).shadowColor(shadow))
        .append(space(-advance))
        .build()
}

private val HOVER_HIDDEN_COLOR = TextColor.color(0x4EB000)

private fun Glyph.centeredVertically(): VAlign = if (height != null) VAlign.MIDDLE else VAlign.TOP

internal fun MenuScope.spanSlots(columns: IntRange, row: Int): List<Int> {
    cell(columns, row)
    return columns.map { column -> session.type.slotIndex(column, row) }
}

internal fun invisibleItem(tooltip: Component?, hover: Component? = null): ItemStack = item(Material.PAPER) {
    itemModel("minecraft:air")
    when {
        hover != null -> {
            customName(Component.text().append(hover).append(tooltip ?: Component.empty()).build())
            // A tooltip that only carries the hover sprite draws no box, with the pack's transparent tooltip style
            val style = MenuFonts.hoverTooltipStyle
            if (tooltip == null && style != null) data(DataComponents.TOOLTIP_STYLE, style)
        }

        tooltip != null -> customName(tooltip)
        else -> noTooltip()
    }
}
