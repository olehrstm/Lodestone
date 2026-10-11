package de.ole101.lodestone.menu.layout

internal object PlayerInventoryLayout {
    const val COLUMNS = GRID_COLUMNS
    const val MAIN_ROWS = 3
    const val ROWS = MAIN_ROWS + 1
    const val MAIN_SLOTS = COLUMNS * MAIN_ROWS
    const val LAST_SLOT = COLUMNS * ROWS - 1
}
