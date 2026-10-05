package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.SlotArea
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.reactive.Derived
import de.ole101.lodestone.reactive.State

/**
 * Shows the items [items] returns page by page in the slots of [slots], each slot configured by [content].
 * Returns the [Pager] that turns the pages. Throws [IllegalArgumentException] if [slots] reaches outside the menu.
 */
public fun <T> MenuScope.paginate(items: () -> List<T>, slots: SlotArea, content: SlotScope.(T) -> Unit): Pager {
    val all = derived(items)
    val areaSlots = slots.slotsIn(session.type)
    val perPage = areaSlots.size
    val pager = Pager(state(0), derived { maxOf(1, Math.ceilDiv(all.value.size, perPage)) })

    element {
        val pageItems = all.value.drop(pager.page * perPage)
        areaSlots.zip(pageItems) { slot, item -> slot(slot) { content(item) } }
    }
    return pager
}

/** The page state of a [paginate] element. Reading its properties in an effect or element tracks them. */
public class Pager internal constructor(private val current: State<Int>, private val count: Derived<Int>) {
    /** The page shown, counted from 0. Values outside `0 until pageCount` are clamped, also after the items shrink. */
    public var page: Int
        get() = current.value.coerceIn(0, pageCount - 1)
        set(value) {
            current.value = value.coerceIn(0, pageCount - 1)
        }

    /** The number of pages, at least 1. */
    public val pageCount: Int get() = count.value

    public val hasNext: Boolean get() = page < pageCount - 1

    public val hasPrevious: Boolean get() = page > 0

    /** Shows the next page, or does nothing on the last one. */
    public fun next() {
        page += 1
    }

    /** Shows the previous page, or does nothing on the first one. */
    public fun previous() {
        page -= 1
    }
}
