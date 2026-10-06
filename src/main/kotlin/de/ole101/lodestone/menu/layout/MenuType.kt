package de.ole101.lodestone.menu.layout

import net.minestom.server.inventory.InventoryType

/**
 * The container a menu is shown in, with its slot grid of [columns] by [rows]. Slots are numbered row by row
 * from the top-left, starting at 0. Cell positions, pixel positions and the title follow the container GUI.
 */
public enum class MenuType(public val columns: Int, public val rows: Int) {
    CHEST_1(9, 1),
    CHEST_2(9, 2),
    CHEST_3(9, 3),
    CHEST_4(9, 4),
    CHEST_5(9, 5),
    CHEST_6(9, 6),

    /** An anvil with its two input slots and the result slot as one row. */
    ANVIL(3, 1);

    /** The number of slots. */
    public val size: Int get() = columns * rows

    public companion object {
        /** Returns the chest type with [rows] rows. Throws [IllegalArgumentException] if [rows] is outside `1..6`. */
        public fun chest(rows: Int): MenuType {
            require(rows in 1..6) { "Chest rows must be within 1..6, but were $rows" }
            return entries[rows - 1]
        }
    }
}

internal val MenuType.inventoryType: InventoryType
    get() = if (this == MenuType.ANVIL) InventoryType.ANVIL else InventoryType.valueOf("CHEST_${rows}_ROW")

/** The GUI x where the title line starts. */
internal val MenuType.titleX: Int
    get() = if (this == MenuType.ANVIL) 60 else MenuMetrics.TITLE_X

/** The GUI x where a left-aligned title starts. */
internal val MenuType.titleLeft: Int
    get() = if (this == MenuType.ANVIL) 60 else MenuMetrics.TITLE_X

/** The height of the container GUI, which the client centers on the screen. */
internal val MenuType.guiHeight: Int
    get() = if (this == MenuType.ANVIL) 166 else 114 + rows * MenuMetrics.SLOT_SIZE

internal fun MenuType.slotX(column: Int): Int =
    if (this == MenuType.ANVIL) intArrayOf(26, 75, 133)[column] else MenuMetrics.cellX(column)

internal fun MenuType.slotY(row: Int): Int = if (this == MenuType.ANVIL) 46 else MenuMetrics.cellY(row)

internal fun MenuType.slotIndex(column: Int, row: Int): Int {
    require(column in 0 until columns) { "Column must be within 0..${columns - 1} for $this, but was $column" }
    require(row in 0 until rows) { "Row must be within 0..${rows - 1} for $this, but was $row" }
    return row * columns + column
}

internal fun MenuType.cellOf(slot: Int): Pair<Int, Int> {
    checkSlot(slot)
    return slot % columns to slot / columns
}

internal fun MenuType.checkSlot(slot: Int) {
    require(slot in 0 until size) { "Slot must be within 0..${size - 1} for $this, but was $slot" }
}

/** Returns the y of the top edge of the slot square in [row] of the player inventory, where row 3 is the hotbar. */
internal fun MenuType.playerSlotY(row: Int): Int {
    val mainTop = if (this == MenuType.ANVIL) 83 else rows * MenuMetrics.SLOT_SIZE + 30
    // The hotbar sits 4 pixels below the main inventory
    return mainTop + row * MenuMetrics.SLOT_SIZE + if (row == 3) 4 else 0
}
