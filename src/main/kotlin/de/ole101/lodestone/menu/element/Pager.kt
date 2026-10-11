package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.SlotArea
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.reactive.node.Derived
import de.ole101.lodestone.reactive.node.State

/**
 * Shows the items [items] returns page by page in the slots of [slots], each slot configured by [content].
 * Returns the [Pager] that turns the pages.
 *
 * @throws IllegalArgumentException if [slots] reaches outside the menu.
 */
public fun <T> MenuScope.paginate(
    items: () -> List<T>,
    slots: SlotArea,
    content: SlotScope.(T) -> Unit,
): Pager {
    val allItems = derived(items)
    val areaSlots = slots.slotsIn(session.type)
    val perPage = areaSlots.size
    val pager = Pager(state(0), derived { maxOf(1, Math.ceilDiv(allItems.value.size, perPage)) })

    element {
        val pageItems = allItems.value.drop(pager.page * perPage)
        areaSlots.zip(pageItems).forEach { (index, item) -> slot(index) { content(item) } }
    }
    return pager
}

/**
 * The page state of a [paginate] element. Reading its properties in an effect or element tracks them.
 */
public class Pager internal constructor(
    private val current: State<Int>,
    private val count: Derived<Int>,
) {
    /**
     * The number of pages, at least 1.
     */
    public val pageCount: Int
        get() = count.value

    /**
     * The page shown, counted from 0. Values outside `0 until pageCount` are clamped, also after the items shrink.
     */
    public var page: Int
        get() = current.value.coerceIn(0, lastPage)
        set(value) {
            current.value = value.coerceIn(0, lastPage)
        }

    public val hasNext: Boolean
        get() = page < lastPage

    public val hasPrevious: Boolean
        get() = page > 0

    public val lastPage: Int
        get() = pageCount - 1

    /**
     * Shows the next page, or does nothing on the last one.
     */
    public fun next() {
        page += 1
    }

    /**
     * Shows the previous page, or does nothing on the first one.
     */
    public fun previous() {
        page -= 1
    }
}
