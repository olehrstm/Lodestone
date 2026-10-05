package de.ole101.lodestone.menu.layout

/**
 * Pixel measurements of the chest menu GUI, in GUI pixels from the top-left corner of the menu.
 */
public object MenuMetrics {
    public const val GUI_WIDTH: Int = 176
    public const val TITLE_X: Int = 8
    public const val TITLE_Y: Int = 6
    public const val SLOT_SIZE: Int = 18

    /** Top-left corner of the first slot square, including its 1 pixel border. Items draw 1 pixel further in. */
    public const val SLOT_ORIGIN_X: Int = 7
    public const val SLOT_ORIGIN_Y: Int = 17

    /** Height of one line of the default font. */
    public const val TEXT_HEIGHT: Int = 8

    /** Returns the x of the left edge of the slot square in [column], counted from 0. */
    public fun cellX(column: Int): Int = SLOT_ORIGIN_X + column * SLOT_SIZE

    /** Returns the y of the top edge of the slot square in [row], counted from 0. */
    public fun cellY(row: Int): Int = SLOT_ORIGIN_Y + row * SLOT_SIZE
}
