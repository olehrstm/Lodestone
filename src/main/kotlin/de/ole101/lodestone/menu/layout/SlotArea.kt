package de.ole101.lodestone.menu.layout

/**
 * A rectangle of slots in the slot grid, used by pagination and scrolling.
 *
 * @see SlotArea.rect
 */
public class SlotArea internal constructor(
    public val columns: IntRange,
    public val rows: IntRange,
) {
    /**
     * Returns the slot indices of this area in [type], row by row from the top-left.
     *
     * @throws IllegalArgumentException if the area does not fit in [type].
     */
    internal fun slotsIn(type: MenuType): List<Int> = rows.flatMap { row -> columns.map { column -> type.slotIndex(column, row) } }

    public companion object {
        /**
         * Returns the slots in [columns] and [rows], both counted from 0, for example `rect(1..7, 1..4)`.
         *
         * @throws IllegalArgumentException if a range is empty, [columns] is outside `0..8`, or [rows] starts below 0.
         */
        public fun rect(
            columns: IntRange,
            rows: IntRange,
        ): SlotArea {
            requireColumns(columns)
            require(!rows.isEmpty() && rows.first >= 0) { "Rows must be a non-empty range starting at 0 or above, but were $rows" }
            return SlotArea(columns, rows)
        }
    }
}

public enum class Axis {
    VERTICAL,
    HORIZONTAL,
}
