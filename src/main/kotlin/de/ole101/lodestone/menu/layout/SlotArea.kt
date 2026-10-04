package de.ole101.lodestone.menu.layout

/**
 * A rectangle of slots in the slot grid, used by pagination and scrolling.
 *
 * @see SlotArea.rect
 */
public class SlotArea internal constructor(public val columns: IntRange, public val rows: IntRange) {
    /** Returns the slot indices of this area in [type], row by row from the top-left. Throws if it does not fit. */
    internal fun slotsIn(type: MenuType): List<Int> = rows.flatMap { row -> columns.map { column -> type.slotIndex(column, row) } }

    public companion object {
        private const val COLUMNS = 9

        /**
         * Returns the slots in [columns] and [rows], both counted from 0, for example `rect(1..7, 1..4)`.
         * Throws [IllegalArgumentException] if a range is empty, [columns] is outside `0..8`, or [rows] starts below 0.
         */
        public fun rect(columns: IntRange, rows: IntRange): SlotArea {
            require(!columns.isEmpty() && columns.first >= 0 && columns.last < COLUMNS) { "Columns must be a non-empty range within 0..8, but were $columns" }
            require(!rows.isEmpty() && rows.first >= 0) { "Rows must be a non-empty range starting at 0 or above, but were $rows" }
            return SlotArea(columns, rows)
        }
    }
}

public enum class Axis {
    VERTICAL,
    HORIZONTAL
}
