package de.ole101.lodestone.menu.element

import de.ole101.lodestone.item.item
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.MenuStyle
import de.ole101.lodestone.menu.layout.Position
import de.ole101.lodestone.menu.layout.VAlign
import de.ole101.lodestone.menu.layout.px
import de.ole101.lodestone.menu.layout.requireColumns
import de.ole101.lodestone.menu.layout.requirePlayerSlot
import de.ole101.lodestone.menu.render.RenderScope
import de.ole101.lodestone.menu.render.centeredVertically
import de.ole101.lodestone.menu.render.requireHeight
import de.ole101.lodestone.menu.render.requireWidth
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * Adds the slot at [index], configured by [block]. The block runs again whenever a state it read changes, for
 * example inside `item { }`.
 *
 * @throws IllegalArgumentException if [index] is outside the menu.
 */
public fun MenuScope.slot(
    index: Int,
    block: SlotScope.() -> Unit,
) {
    session.type.checkSlot(index)
    element { slot(index, block) }
}

public fun MenuScope.slot(
    column: Int,
    row: Int,
    block: SlotScope.() -> Unit,
) {
    slot(session.type.slotIndex(column, row), block)
}

/**
 * Adds a slot of each viewer's own inventory at [column] and [row], shown instead of their item while the menu is
 * open. Rows 0 to 2 are the main inventory from the top and row 3 is the hotbar.
 *
 * @throws IllegalArgumentException if the slot is outside 9 by 4.
 * @see RenderScope.playerSlot
 */
public fun MenuScope.playerSlot(
    column: Int,
    row: Int,
    block: SlotScope.() -> Unit,
) {
    requirePlayerSlot(column, row)
    element { playerSlot(column, row, block) }
}

/**
 * Draws [glyph] in the title at [at], aligned with [align] and, inside a cell, [vAlign], tinted with [color].
 * Requires the GUI text shader that decodes relative shadow markers. Tint is quantized to RGB332. Rendering throws
 * [IllegalArgumentException] if the vertical offset, including the ascent correction, is outside `-2048..2047`.
 *
 * @throws IllegalArgumentException if the glyph has no [Glyph.width], or no [Glyph.height] when [vAlign] is not
 * [VAlign.TOP].
 * @see RenderScope.glyph
 */
public fun MenuScope.glyph(
    glyph: Glyph,
    at: Position,
    align: TextAlign = TextAlign.LEFT,
    vAlign: VAlign = VAlign.TOP,
    color: TextColor = NamedTextColor.WHITE,
) {
    glyph.requireWidth()
    glyph.requireHeight(vAlign)
    element { glyph(glyph, at, align, vAlign, color) }
}

/**
 * Draws a full-width sprite behind rows of the menu. [glyph] returns the sprite for each row, counted from 0, or null
 * to leave the row as it is. Each sprite's top-left corner is at the left edge of the menu and the top of the row's
 * slots, so it fits sprites as wide as [MenuMetrics.GUI_WIDTH]. Call it before other glyphs to draw it below them.
 *
 * @throws IllegalArgumentException if a glyph has no [Glyph.width].
 */
public fun MenuScope.rowGlyphs(glyph: (row: Int) -> Glyph?) {
    for (row in 0 until session.type.rows) {
        val rowGlyph = glyph(row) ?: continue
        glyph(rowGlyph, px(0, session.type.gui.slotY(row)))
    }
}

/**
 * Draws the default-font text that [text] returns in the title, and redraws it when a state it read changes.
 * Requires the GUI text shader for relative shadow markers. Colors use RGB332 precision and replace normal shadows.
 * Rendering throws [IllegalArgumentException] if the vertical offset is outside `-2048..2047`.
 *
 * @see RenderScope.text
 */
public fun MenuScope.text(
    at: Position,
    align: TextAlign = TextAlign.LEFT,
    vAlign: VAlign = VAlign.TOP,
    text: () -> Component,
) {
    element { text(at, align, vAlign, text) }
}

/**
 * Draws [text] as the title, aligned with [align] inside the menu width. Left-aligned titles start where the
 * container's own title would. [y] is the GUI y of the text's top, by default where the container's own title is,
 * so `y = MenuMetrics.TITLE_Y + 2` moves it 2 pixels down. The title draws on top of all other text and glyphs.
 * Requires the GUI text shader for relative shadow markers. Colors use RGB332 precision and replace normal shadows.
 */
public fun MenuScope.title(
    align: TextAlign = TextAlign.LEFT,
    y: Int = MenuMetrics.TITLE_Y,
    text: () -> Component,
) {
    element { title(text(), align, y) }
}

/**
 * Draws [glyph] centered over the cells [at], also vertically when the glyph has a height.
 */
internal fun RenderScope.spanGlyph(
    glyph: Glyph,
    at: Position,
) {
    glyph(glyph, at, TextAlign.CENTER, glyph.centeredVertically())
}

internal fun MenuScope.spanSlots(
    columns: IntRange,
    row: Int,
): List<Int> {
    requireColumns(columns)
    return columns.map { column -> session.type.slotIndex(column, row) }
}

internal fun invisibleItem(
    tooltip: Component?,
    hover: Component? = null,
): ItemStack = item(Material.PAPER) {
    itemModel("minecraft:air")
    when {
        hover != null -> {
            customName(Component.textOfChildren(hover, tooltip ?: Component.empty()))
            // A tooltip that only carries the hover sprite draws no box, with the pack's transparent tooltip style
            val style = MenuStyle.hoverTooltipStyle
            if (tooltip == null && style != null) data(DataComponents.TOOLTIP_STYLE, style)
        }

        tooltip != null -> {
            customName(tooltip)
        }

        else -> {
            hideTooltip()
        }
    }
}
