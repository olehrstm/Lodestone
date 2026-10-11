package de.ole101.lodestone.menu.layout

import net.minestom.server.inventory.InventoryType

/**
 * The container a menu is shown in, with its slot grid of [columns] by [rows]. Slots are numbered row by row
 * from the top-left, starting at 0. Cell positions, pixel positions and the title follow the container GUI.
 */
public enum class MenuType(
    public val columns: Int,
    public val rows: Int,
    internal val inventoryType: InventoryType,
    internal val gui: GuiLayout = GuiLayout.chest(rows),
) {
    CHEST_1(GRID_COLUMNS, 1, InventoryType.CHEST_1_ROW),
    CHEST_2(GRID_COLUMNS, 2, InventoryType.CHEST_2_ROW),
    CHEST_3(GRID_COLUMNS, 3, InventoryType.CHEST_3_ROW),
    CHEST_4(GRID_COLUMNS, 4, InventoryType.CHEST_4_ROW),
    CHEST_5(GRID_COLUMNS, 5, InventoryType.CHEST_5_ROW),
    CHEST_6(GRID_COLUMNS, 6, InventoryType.CHEST_6_ROW),

    /**
     * An anvil with its two input slots and the result slot as one row.
     */
    ANVIL(3, 1, InventoryType.ANVIL, GuiLayout.ANVIL),
    ;

    public val size: Int get() = columns * rows

    internal fun slotIndex(
        column: Int,
        row: Int,
    ): Int {
        require(column in 0 until columns) { "Column must be within 0..${columns - 1} for $this, but was $column" }
        require(row in 0 until rows) { "Row must be within 0..${rows - 1} for $this, but was $row" }
        return row * columns + column
    }

    internal fun cellOf(slot: Int): Pair<Int, Int> {
        checkSlot(slot)
        return slot % columns to slot / columns
    }

    internal fun checkSlot(slot: Int) = require(slot in 0 until size) { "Slot must be within 0..${size - 1} for $this, but was $slot" }

    public companion object {
        /**
         * Returns the chest type with [rows] rows.
         *
         * @throws IllegalArgumentException if [rows] is outside `1..6`.
         */
        public fun chest(rows: Int): MenuType {
            require(rows in 1..6) { "Chest rows must be within 1..6, but were $rows" }
            return entries[rows - 1]
        }
    }
}

/**
 * The number of columns of chests and the player inventory.
 */
internal const val GRID_COLUMNS = 9
