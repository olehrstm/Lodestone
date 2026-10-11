package de.ole101.lodestone.menu.slot

import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack

/**
 * What players may do with the item in a menu slot.
 */
public enum class SlotPolicy(
    internal val canTake: Boolean,
    internal val canPlace: Boolean,
) {
    /**
     * Every click is cancelled.
     */
    LOCKED(canTake = false, canPlace = false),

    /**
     * Players may take the item out, but not put items in.
     */
    TAKE(canTake = true, canPlace = false),

    /**
     * Players may put items in, for example an input slot, but not take them out.
     */
    PLACE(canTake = false, canPlace = true),

    /**
     * Players may take items out and put items in.
     */
    FREE(canTake = true, canPlace = true),
}

/**
 * Returns whether [click] on a slot holding [slotItem] follows this policy. [cursor] is the item on the cursor,
 * [swapped] the hotbar or offhand item for swap clicks, and [accepts] decides which items may be placed.
 */
internal fun SlotPolicy.allows(
    click: Click,
    slotItem: ItemStack,
    cursor: ItemStack,
    swapped: ItemStack,
    accepts: (ItemStack) -> Boolean,
): Boolean = when (click) {
    is Click.Left, is Click.Right -> allowsPrimaryClick(slotItem, cursor, accepts)

    is Click.LeftShift, is Click.RightShift, is Click.DropSlot -> canTake

    is Click.HotbarSwap, is Click.OffhandSwap -> allowsSwap(slotItem, swapped, accepts)

    // Double clicks and drags involve several slots and are checked by the session, while middle clicks are creative only.
    is Click.Double, is Click.Drag, is Click.Middle, is Click.DropCursor -> false
}

/**
 * Returns whether [item] may be put into a slot holding [slotItem]. Only an empty slot or a matching stack accepts it.
 */
internal fun SlotPolicy.allowsPlacing(
    slotItem: ItemStack,
    item: ItemStack,
    accepts: (ItemStack) -> Boolean,
): Boolean = canPlace && accepts(item) && (slotItem.isAir || slotItem.isSimilar(item))

/**
 * Returns whether a double click holding [cursor] may collect from a slot holding [slotItem].
 */
internal fun SlotPolicy?.allowsCollecting(
    slotItem: ItemStack,
    cursor: ItemStack,
): Boolean = this?.canTake == true || !slotItem.isSimilar(cursor)

private fun SlotPolicy.allowsPrimaryClick(
    slotItem: ItemStack,
    cursor: ItemStack,
    accepts: (ItemStack) -> Boolean,
): Boolean = when {
    cursor.isAir -> canTake && !slotItem.isAir
    slotItem.isAir || cursor.isSimilar(slotItem) -> canPlace && accepts(cursor)
    else -> canTake && canPlace && accepts(cursor)
}

private fun SlotPolicy.allowsSwap(
    slotItem: ItemStack,
    swapped: ItemStack,
    accepts: (ItemStack) -> Boolean,
): Boolean = (slotItem.isAir || canTake) && (swapped.isAir || (canPlace && accepts(swapped)))
