package de.ole101.lodestone.menu.render

import de.ole101.lodestone.menu.MenuDsl
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.layout.*
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.menu.slot.SlotView
import de.ole101.lodestone.reactive.EffectScope
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
import de.ole101.lodestone.text.glyph.anchor
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * Draws one element of a menu. Every render starts empty and replaces what the element drew before.
 * States read while rendering are tracked, so the element renders again when they change.
 *
 * @see MenuElement
 */
@MenuDsl
public class RenderScope internal constructor(
    /** The session this element renders in, for example to read [MenuSession.viewers] or slot contents. */
    public val session: MenuSession,
    private val coroutineScope: CoroutineScope,
    private val effect: EffectScope,
) {
    private val type: MenuType = session.type
    internal val slots = LinkedHashMap<Int, SlotView>()
    internal val layers = mutableListOf<TitleLayer>()

    /**
     * Fills the slot at [index] as configured in [block]. A later element that fills the same slot wins.
     * Throws [IllegalArgumentException] if [index] is outside the menu.
     */
    public fun slot(index: Int, block: SlotScope.() -> Unit) {
        type.checkSlot(index)
        slots[index] = SlotScope(session, coroutineScope).apply(block).build()
    }

    public fun slot(column: Int, row: Int, block: SlotScope.() -> Unit) {
        slot(type.slotIndex(column, row), block)
    }

    /**
     * Draws [glyph] in the title, tinted with [color]. The default white shows the texture as it is. With a
     * [Position.Pixel], its x is the left edge, center or right edge depending on [align], and its y the top. With a
     * [Position.Cell], the glyph is aligned inside the slot squares with [align] and [vAlign]. [vAlign] does nothing
     * for pixel positions. Throws [IllegalArgumentException] if the glyph has no [Glyph.width], no [Glyph.height] when
     * [vAlign] is not [VAlign.TOP] in a cell, or the cell is outside the menu.
     */
    public fun glyph(
        glyph: Glyph,
        at: Position,
        align: TextAlign = TextAlign.LEFT,
        vAlign: VAlign = VAlign.TOP,
        color: TextColor = NamedTextColor.WHITE,
    ) {
        val (x, y) = glyphPosition(glyph, at, align, vAlign)
        layers += TitleLayer.GlyphLayer(glyph, x, y, color)
    }

    internal fun glyphPosition(glyph: Glyph, at: Position, align: TextAlign, vAlign: VAlign): Pair<Int, Int> {
        val width = requireNotNull(glyph.width) { "Glyph needs a width to be drawn in a menu, but $glyph has none" }
        return when (at) {
            is Position.Pixel -> at.x - align.anchor(width) to at.y
            is Position.Cell -> {
                val height = if (vAlign == VAlign.TOP) 0 else requireNotNull(glyph.height) { "Glyph needs a height to be aligned $vAlign, but $glyph has none" }
                anchorX(at, align) - align.anchor(width) to alignedY(at.row, height, vAlign)
            }
        }
    }

    /**
     * Draws the default-font [text] in the title. With a [Position.Pixel], its x is the left edge, center or right
     * edge depending on [align], and its y the top. With a [Position.Cell], the text is aligned inside the cells
     * with [align] and [vAlign]. [vAlign] does nothing for pixel positions.
     */
    public fun text(at: Position, align: TextAlign = TextAlign.LEFT, vAlign: VAlign = VAlign.TOP, text: () -> Component) {
        layers += when (at) {
            is Position.Pixel -> TitleLayer.TextLayer(text(), at.x, at.y, align)
            is Position.Cell -> TitleLayer.TextLayer(text(), anchorX(at, align), alignedY(at.row, MenuMetrics.TEXT_HEIGHT, vAlign), align)
        }
    }

    /** Runs [block] before the next render of this element and when the element is removed. */
    public fun onCleanup(block: () -> Unit) {
        effect.onCleanup(block)
    }

    // Also for slots that are not next to each other
    private fun anchorX(cell: Position.Cell, align: TextAlign): Int {
        type.slotIndex(cell.columns.last, cell.row)
        val left = type.slotX(cell.columns.first)
        return left + align.anchor(type.slotX(cell.columns.last) + MenuMetrics.SLOT_SIZE - left)
    }

    private fun alignedY(row: Int, height: Int, vAlign: VAlign): Int {
        val free = MenuMetrics.SLOT_SIZE - height
        return type.slotY(row) + when (vAlign) {
            VAlign.TOP -> 0
            VAlign.MIDDLE -> free / 2
            VAlign.BOTTOM -> free
        }
    }

    internal fun title(text: Component, align: TextAlign) {
        val x = when (align) {
            TextAlign.LEFT -> type.titleLeft
            TextAlign.CENTER -> MenuMetrics.GUI_WIDTH / 2
            TextAlign.RIGHT -> MenuMetrics.GUI_WIDTH - MenuMetrics.TITLE_X
        }
        layers += TitleLayer.TextLayer(text, x, MenuMetrics.TITLE_Y, align, onTop = true)
    }
}

/** A custom menu element, added with [MenuScope.element]. */
public interface MenuElement {
    /** Draws this element. Runs on the tick thread each time a state it read changes. */
    public fun RenderScope.render()
}
