package de.ole101.lodestone.menu.render

import de.ole101.lodestone.LodestoneDsl
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.PlayerInventoryLayout
import de.ole101.lodestone.menu.layout.Position
import de.ole101.lodestone.menu.layout.VAlign
import de.ole101.lodestone.menu.layout.requirePlayerSlot
import de.ole101.lodestone.menu.slot.ClickContext
import de.ole101.lodestone.menu.slot.SlotPolicy
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.menu.slot.SlotView
import de.ole101.lodestone.reactive.node.EffectScope
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
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
@LodestoneDsl
public class RenderScope internal constructor(
    /**
     * The session this element renders in, for example to read [MenuSession.viewers] or slot contents.
     */
    public val session: MenuSession,
    private val coroutineScope: CoroutineScope,
    private val effect: EffectScope,
) {
    private val type = session.type
    internal val slots = LinkedHashMap<Int, SlotView>()
    internal val layers = mutableListOf<TitleLayer>()
    internal val playerSlots = LinkedHashMap<Int, SlotView>()

    /**
     * Fills the slot at [index] as configured in [block]. A later element that fills the same slot wins.
     *
     * @throws IllegalArgumentException if [index] is outside the menu.
     */
    public fun slot(
        index: Int,
        block: SlotScope.() -> Unit,
    ) {
        type.checkSlot(index)
        slots[index] = buildSlot(block)
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
     *
     * @throws IllegalArgumentException if the slot is outside 9 by 4 or its policy is not [SlotPolicy.LOCKED].
     */
    public fun playerSlot(
        column: Int,
        row: Int,
        block: SlotScope.() -> Unit,
    ) {
        requirePlayerSlot(column, row)
        val view = buildSlot(block)
        require(view.policy == SlotPolicy.LOCKED) { "Player inventory slots must be LOCKED, but were ${view.policy}" }
        playerSlots[row * PlayerInventoryLayout.COLUMNS + column] = view
    }

    /**
     * Draws [glyph] in the title, tinted with [color]. The default white shows the texture as it is. With a
     * [Position.Pixel], its x is the left edge, center or right edge depending on [align], and its y the top. With a
     * [Position.Cell] or [Position.PlayerCell], the glyph is aligned inside the slot squares with [align] and [vAlign].
     * [vAlign] does nothing for pixel positions.
     *
     * @throws IllegalArgumentException if the glyph has no [Glyph.width], no [Glyph.height] when [vAlign] is not
     * [VAlign.TOP] in a cell, or the cell is outside the menu.
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
        val (x, y) = anchorOf(at, align, vAlign) { MenuMetrics.TEXT_HEIGHT }
        layers += TitleLayer.TextLayer(text(), x, y, align)
    }

    /**
     * Runs [block] before the next render of this element and when the element is removed.
     */
    public fun onCleanup(block: () -> Unit) {
        effect.onCleanup(block)
    }

    internal fun glyphPosition(
        glyph: Glyph,
        at: Position,
        align: TextAlign,
        vAlign: VAlign,
    ): Pair<Int, Int> {
        val width = glyph.requireWidth()
        val (x, y) = anchorOf(at, align, vAlign) { glyph.requireHeight(vAlign) }
        return x - align.anchor(width) to y
    }

    internal fun title(
        text: Component,
        align: TextAlign,
        y: Int,
    ) {
        val x = when (align) {
            TextAlign.LEFT -> type.gui.titleX
            TextAlign.CENTER -> MenuMetrics.GUI_WIDTH / 2
            TextAlign.RIGHT -> MenuMetrics.GUI_WIDTH - MenuMetrics.TITLE_X
        }
        layers += TitleLayer.TextLayer(text, x, y, align, onTop = true)
    }

    private fun buildSlot(block: SlotScope.() -> Unit) = SlotScope(session, coroutineScope).apply(block).build()

    // The height is only needed in cells, so pixel positions never ask for it
    private inline fun anchorOf(
        at: Position,
        align: TextAlign,
        vAlign: VAlign,
        height: () -> Int,
    ): Pair<Int, Int> {
        if (at is Position.Pixel) return at.x to at.y

        val box = boxOf(at)
        return box.anchorX(align) to box.alignedY(height(), vAlign)
    }

    // The slot squares a cell covers, from the left edge of the first slot to the right edge of the last
    private fun boxOf(at: Position): SlotBox = when (at) {
        is Position.Pixel -> {
            error("A pixel position covers no slots")
        }

        is Position.Cell -> {
            type.slotIndex(at.columns.last, at.row) // Throws if the cell is outside the menu
            SlotBox(type.gui.slotX(at.columns.first), type.gui.slotX(at.columns.last) + MenuMetrics.SLOT_SIZE, type.gui.slotY(at.row))
        }

        is Position.PlayerCell -> {
            SlotBox(
                MenuMetrics.cellX(at.columns.first),
                MenuMetrics.cellX(at.columns.last) + MenuMetrics.SLOT_SIZE,
                type.gui.playerSlotY(at.row),
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
            return top + when (vAlign) {
                VAlign.TOP -> 0
                VAlign.MIDDLE -> free / 2
                VAlign.BOTTOM -> free
            }
        }
    }
}

/**
 * A custom menu element, added with [MenuScope.element].
 */
public interface MenuElement {
    /**
     * Draws this element. Runs on the tick thread each time a state it read changes.
     */
    public fun RenderScope.render()
}

internal fun Glyph.requireWidth() = requireNotNull(width) { "Glyph needs a width to be drawn in a menu, but $this has none" }

/**
 * Returns the height that aligning with [vAlign] needs, which is 0 for [VAlign.TOP].
 */
internal fun Glyph.requireHeight(vAlign: VAlign): Int {
    if (vAlign == VAlign.TOP) return 0
    return requireNotNull(height) { "Glyph needs a height to be aligned $vAlign, but $this has none" }
}

internal fun Glyph.centeredVertically() = if (height != null) VAlign.MIDDLE else VAlign.TOP
