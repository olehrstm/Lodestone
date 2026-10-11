package de.ole101.lodestone.menu.layout

/**
 * Where a container GUI draws its title and slots, in GUI pixels from its top-left corner.
 */
internal class GuiLayout(
    val titleX: Int,
    // The client centers the GUI on the screen by this height
    val height: Int,
    private val slotXs: IntArray,
    private val slotTop: Int,
    private val playerInventoryTop: Int,
) {
    fun slotX(column: Int) = slotXs[column]

    fun slotY(row: Int) = slotTop + row * MenuMetrics.SLOT_SIZE

    /**
     * Returns the y of the top edge of the slot square in [row] of the player inventory, where row 3 is the hotbar.
     */
    fun playerSlotY(row: Int): Int {
        val hotbarGap = if (row == PlayerInventoryLayout.MAIN_ROWS) HOTBAR_GAP else 0
        return playerInventoryTop + row * MenuMetrics.SLOT_SIZE + hotbarGap
    }

    companion object {
        val ANVIL = GuiLayout(
            titleX = 60,
            height = 166,
            slotXs = intArrayOf(26, 75, 133),
            slotTop = 46,
            playerInventoryTop = 83,
        )

        fun chest(rows: Int) = GuiLayout(
            titleX = MenuMetrics.TITLE_X,
            height = CHEST_BASE_HEIGHT + rows * MenuMetrics.SLOT_SIZE,
            slotXs = IntArray(CHEST_COLUMNS, MenuMetrics::cellX),
            slotTop = MenuMetrics.SLOT_ORIGIN_Y,
            playerInventoryTop = rows * MenuMetrics.SLOT_SIZE + CHEST_PLAYER_INVENTORY_OFFSET,
        )
    }
}

private const val CHEST_COLUMNS = 9

// The chest GUI is this high without its slot rows, and its player inventory starts this far below them
private const val CHEST_BASE_HEIGHT = 114
private const val CHEST_PLAYER_INVENTORY_OFFSET = 30

// The hotbar sits 4 pixels below the main inventory
private const val HOTBAR_GAP = 4
