package de.ole101.lodestone.menu.render

import de.ole101.lodestone.menu.MenuDsl
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.layout.Position
import de.ole101.lodestone.menu.layout.VAlign
import de.ole101.lodestone.menu.layout.checkSlot
import de.ole101.lodestone.menu.layout.playerSlotY
import de.ole101.lodestone.menu.layout.slotIndex
import de.ole101.lodestone.menu.layout.slotX
import de.ole101.lodestone.menu.layout.slotY
import de.ole101.lodestone.menu.layout.titleLeft
import de.ole101.lodestone.menu.slot.ClickContext
import de.ole101.lodestone.menu.slot.SlotPolicy
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
    internal val playerSlots = LinkedHashMap<Int, SlotView>()

    /**
     * Fills the slot at [index] as configured in [block]. A later element that fills the same slot wins.
     * Throws [IllegalArgumentException] if [index] is outside the menu.
     */
    public fun slot(
        index: Int,
        block: SlotScope.() -> Unit,
    ) {
        type.checkSlot(index)
        slots[index] = SlotScope(session, coroutineScope).apply(block).build()
    }

    public fun slot(
        column: Int,
        row: Int,
        block: SlotScope.() -> Unit,
    ) {
        slot(type.slotIndex(column, row), block)
    }

    /**
     * Fills the slot at [column] and [row] of each viewer's own inventory, as configured in [block]. Rows 0 to 2 are
     * the main inventory from the top and row 3 is the hotbar. Viewers see this item instead of their own while the
     * menu is open, and their real items stay untouched. [ClickContext.slot] is `row * 9 + column`.
     * Throws [IllegalArgumentException] if the slot is outside 9 by 4 or its policy is not [SlotPolicy.LOCKED].
     */
    public fun playerSlot(
        column: Int,
        row: Int,
        block: SlotScope.() -> Unit,
    ) {
        requirePlayerSlot(column, row)
        val view = SlotScope(session, coroutineScope).apply(block).build()
        require(view.policy == SlotPolicy.LOCKED) { "Player inventory slots must be LOCKED, but were ${view.policy}" }
        playerSlots[row * 9 + column] = view
    }

    /**
     * Draws [glyph] in the title, tinted with [color]. The default white shows the texture as it is. With a
     * [Position.Pixel], its x is the left edge, center or right edge depending on [align], and its y the top. With a
     * [Position.Cell] or [Position.PlayerCell], the glyph is aligned inside the slot squares with [align] and [vAlign]. [vAlign] does nothing
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

    internal fun glyphPosition(
        glyph: Glyph,
        at: Position,
        align: TextAlign,
        vAlign: VAlign,
    ): Pair<Int, Int> {
        val width = requireNotNull(glyph.width) { "Glyph needs a width to be drawn in a menu, but $glyph has none" }
        val box = boxOf(at) ?: return (at as Position.Pixel).x - align.anchor(width) to at.y
        val height =
            if (vAlign ==
                VAlign.TOP
            ) {
                0
            } else {
                requireNotNull(glyph.height) { "Glyph needs a height to be aligned $vAlign, but $glyph has none" }
            }
        return box.anchorX(align) - align.anchor(width) to box.alignedY(height, vAlign)
    }

    /**
     * Draws the default-font [text] in the title. With a [Position.Pixel], its x is the left edge, center or right
     * edge depending on [align], and its y the top. With a [Position.Cell] or [Position.PlayerCell], the text is
     * aligned inside the cells with [align] and [vAlign]. [vAlign] does nothing for pixel positions.
     */
    public fun text(
        at: Position,
        align: TextAlign = TextAlign.LEFT,
        vAlign: VAlign = VAlign.TOP,
        text: () -> Component,
    ) {
        val box = boxOf(at)
        layers +=
            if (box == null) {
                at as Position.Pixel
                TitleLayer.TextLayer(text(), at.x, at.y, align)
            } else {
                TitleLayer.TextLayer(text(), box.anchorX(align), box.alignedY(MenuMetrics.TEXT_HEIGHT, vAlign), align)
            }
    }

    /** Runs [block] before the next render of this element and when the element is removed. */
    public fun onCleanup(block: () -> Unit) {
        effect.onCleanup(block)
    }

    // The slot squares a cell covers, from the left edge of the first slot to the right edge of the last
    private fun boxOf(at: Position): SlotBox? =
        when (at) {
            is Position.Pixel -> {
                null
            }

            is Position.Cell -> {
                type.slotIndex(at.columns.last, at.row)
                SlotBox(type.slotX(at.columns.first), type.slotX(at.columns.last) + MenuMetrics.SLOT_SIZE, type.slotY(at.row))
            }

            is Position.PlayerCell -> {
                SlotBox(
                    MenuMetrics.cellX(at.columns.first),
                    MenuMetrics.cellX(at.columns.last) + MenuMetrics.SLOT_SIZE,
                    type.playerSlotY(at.row),
                )
            }
        }

    private class SlotBox(
        val left: Int,
        val right: Int,
        val top: Int,
    ) {
        fun anchorX(align: TextAlign): Int = left + align.anchor(right - left)

        fun alignedY(
            height: Int,
            vAlign: VAlign,
        ): Int {
            val free = MenuMetrics.SLOT_SIZE - height
            return top +
                when (vAlign) {
                    VAlign.TOP -> 0
                    VAlign.MIDDLE -> free / 2
                    VAlign.BOTTOM -> free
                }
        }
    }

    internal fun title(
        text: Component,
        align: TextAlign,
        y: Int,
    ) {
        val x =
            when (align) {
                TextAlign.LEFT -> type.titleLeft
                TextAlign.CENTER -> MenuMetrics.GUI_WIDTH / 2
                TextAlign.RIGHT -> MenuMetrics.GUI_WIDTH - MenuMetrics.TITLE_X
            }
        layers += TitleLayer.TextLayer(text, x, y, align, onTop = true)
    }
}

/** A custom menu element, added with [MenuScope.element]. */
public interface MenuElement {
    /** Draws this element. Runs on the tick thread each time a state it read changes. */
    public fun RenderScope.render()
}

internal fun requirePlayerSlot(
    column: Int,
    row: Int,
) {
    require(column in 0..8) { "Player inventory column must be within 0..8, but was $column" }
    require(row in 0..3) { "Player inventory row must be within 0..3, but was $row" }
}
