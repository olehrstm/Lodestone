package de.ole101.lodestone.menu.slot

import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack

/** What players may do with the item in a menu slot. */
public enum class SlotPolicy {
    /** Every click is cancelled. */
    LOCKED,

    /** Players may take the item out, but not put items in. */
    TAKE,

    /** Players may put items in, for example an input slot, but not take them out. */
    PLACE,

    /** Players may take items out and put items in. */
    FREE,
}

internal val SlotPolicy.canTake: Boolean get() = this == SlotPolicy.TAKE || this == SlotPolicy.FREE

internal val SlotPolicy.canPlace: Boolean get() = this == SlotPolicy.PLACE || this == SlotPolicy.FREE

/**
 * Returns whether [click] on a slot holding [slotItem] follows this policy. [cursor] is the item on the cursor,
 * [swapped] the hotbar or offhand item for swap clicks, and [accepts] decides which items may be placed.
 */
internal fun SlotPolicy.allows(click: Click, slotItem: ItemStack, cursor: ItemStack, swapped: ItemStack, accepts: (ItemStack) -> Boolean): Boolean {
    fun placeable(item: ItemStack) = canPlace && accepts(item)

    return when (click) {
        is Click.Left, is Click.Right -> when {
            cursor.isAir -> canTake && !slotItem.isAir
            slotItem.isAir || cursor.isSimilar(slotItem) -> placeable(cursor)
            else -> canTake && placeable(cursor)
        }

        is Click.LeftShift, is Click.RightShift, is Click.DropSlot -> canTake
        is Click.HotbarSwap, is Click.OffhandSwap -> (slotItem.isAir || canTake) && (swapped.isAir || placeable(swapped))
        // Double clicks and drags involve several slots and are checked by the session; middle clicks are creative only
        else -> false
    }
}
