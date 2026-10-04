package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.Axis
import de.ole101.lodestone.menu.layout.SlotArea
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.reactive.Derived
import de.ole101.lodestone.reactive.State

/**
 * Shows the items [items] returns in the slots of [area], scrolled by whole lines along [axis], each slot
 * configured by [content]. Vertical scrolling fills rows left to right, horizontal scrolling fills columns top to
 * bottom. Returns the [Scroller] that moves the lines. Throws [IllegalArgumentException] if [area] reaches outside
 * the menu.
 */
public fun <T> MenuScope.scroll(items: () -> List<T>, area: SlotArea, axis: Axis = Axis.VERTICAL, content: SlotScope.(T) -> Unit): Scroller {
    val all = derived(items)
    val areaSlots = area.slotsIn(session.type)
    val columns = area.columns.count()
    val rows = area.rows.count()
    val lineLength = if (axis == Axis.VERTICAL) columns else rows
    val visibleLines = if (axis == Axis.VERTICAL) rows else columns
    val scroller = Scroller(state(0), derived { maxOf(0, Math.ceilDiv(all.value.size, lineLength) - visibleLines) })

    element {
        val list = all.value
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val (line, position) = if (axis == Axis.VERTICAL) row to column else column to row
                val index = (scroller.offset + line) * lineLength + position
                if (index < list.size) slot(areaSlots[row * columns + column]) { content(list[index]) }
            }
        }
    }
    return scroller
}

/** The scroll state of a [scroll] element. Reading its properties in an effect or element tracks them. */
public class Scroller internal constructor(private val current: State<Int>, private val maxOffset: Derived<Int>) {
    /** The number of lines scrolled past, within `0..max`. */
    public val offset: Int get() = current.value.coerceIn(0, max)

    /** The largest [offset], where the last line is visible. */
    public val max: Int get() = maxOffset.value

    /** Scrolls by [lines], where negative values scroll back. Stops at `0` and [max]. */
    public fun by(lines: Int) {
        to(offset + lines)
    }

    /** Scrolls to [offset], clamped to `0..max`. */
    public fun to(offset: Int) {
        current.value = offset.coerceIn(0, max)
    }
}
