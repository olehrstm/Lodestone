package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.Axis
import de.ole101.lodestone.menu.layout.SlotArea
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.reactive.node.Derived
import de.ole101.lodestone.reactive.node.State

/**
 * Shows the items [items] returns in the slots of [area], scrolled by whole lines along [axis], each slot
 * configured by [content]. Vertical scrolling fills rows left to right, horizontal scrolling fills columns top to
 * bottom. Returns the [Scroller] that moves the lines.
 *
 * @throws IllegalArgumentException if [area] reaches outside the menu.
 */
public fun <T> MenuScope.scroll(
    items: () -> List<T>,
    area: SlotArea,
    axis: Axis = Axis.VERTICAL,
    content: SlotScope.(T) -> Unit,
): Scroller {
    val allItems = derived(items)
    val areaSlots = area.slotsIn(session.type)
    val columns = area.columns.count()
    val rows = area.rows.count()
    val isVertical = axis == Axis.VERTICAL
    val lineLength = if (isVertical) columns else rows
    val visibleLines = if (isVertical) rows else columns
    val scroller = Scroller(state(0), derived { maxOf(0, Math.ceilDiv(allItems.value.size, lineLength) - visibleLines) })

    element {
        val list = allItems.value
        val offset = scroller.offset
        areaSlots.forEachIndexed { cell, areaSlot ->
            val row = cell / columns
            val column = cell % columns
            val (line, position) = if (isVertical) row to column else column to row
            val index = (offset + line) * lineLength + position
            if (index < list.size) slot(areaSlot) { content(list[index]) }
        }
    }
    return scroller
}

/**
 * The scroll state of a [scroll] element. Reading its properties in an effect or element tracks them.
 */
public class Scroller internal constructor(
    private val current: State<Int>,
    private val maxOffset: Derived<Int>,
) {
    /**
     * The largest [offset], where the last line is visible.
     */
    public val max: Int
        get() = maxOffset.value

    /**
     * The number of lines scrolled past. Values outside `0..max` are clamped, also after the items shrink.
     */
    public var offset: Int
        get() = current.value.coerceIn(0, max)
        set(value) {
            current.value = value.coerceIn(0, max)
        }

    /**
     * Scrolls one line forward, or does nothing at [max].
     */
    public fun next() {
        offset += 1
    }

    /**
     * Scrolls one line back, or does nothing at the start.
     */
    public fun previous() {
        offset -= 1
    }
}
