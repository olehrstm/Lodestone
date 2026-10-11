package de.ole101.lodestone.menu.layout

/**
 * Where text or a glyph is drawn in a menu, at a pixel with [px], in the slot grid with [cell], or over the viewer's
 * own inventory with [playerCell].
 */
public sealed interface Position {
    public class Pixel internal constructor(
        public val x: Int,
        public val y: Int,
    ) : Position

    public class Cell internal constructor(
        public val columns: IntRange,
        public val row: Int,
    ) : Position

    public class PlayerCell internal constructor(
        public val columns: IntRange,
        public val row: Int,
    ) : Position
}

public enum class VAlign {
    TOP,
    MIDDLE,
    BOTTOM,
}

/**
 * Returns the point [x], [y] in GUI pixels from the top-left corner of the menu.
 */
public fun px(
    x: Int,
    y: Int,
): Position = Position.Pixel(x, y)

/**
 * Returns the slot at [column] and [row], both counted from 0.
 *
 * @throws IllegalArgumentException if [column] is outside `0..8` or [row] is negative.
 */
public fun cell(
    column: Int,
    row: Int,
): Position = cell(column..column, row)

/**
 * Returns the slots in [columns] of [row], for example `cell(0..8, 5)` for the whole sixth row.
 *
 * @throws IllegalArgumentException if [columns] is empty or outside `0..8`, or [row] is negative.
 */
public fun cell(
    columns: IntRange,
    row: Int,
): Position {
    requireColumns(columns)
    require(row >= 0) { "Row must not be negative, but was $row" }
    return Position.Cell(columns, row)
}

/**
 * Returns the slot at [column] and [row] of the viewer's own inventory. Rows 0 to 2 are the main inventory from the
 * top and row 3 is the hotbar.
 *
 * @throws IllegalArgumentException if the slot is outside 9 by 4.
 */
public fun playerCell(
    column: Int,
    row: Int,
): Position = playerCell(column..column, row)

/**
 * Returns the slots in [columns] of [row] of the viewer's own inventory. See the `playerCell` overload with one column.
 *
 * @throws IllegalArgumentException if [columns] is empty or outside `0..8`, or [row] is outside `0..3`.
 */
public fun playerCell(
    columns: IntRange,
    row: Int,
): Position {
    requireColumns(columns)
    requirePlayerRow(row)
    return Position.PlayerCell(columns, row)
}

private const val MAX_COLUMNS = 9

internal fun requireColumns(columns: IntRange) = require(!columns.isEmpty() && columns.first >= 0 && columns.last < MAX_COLUMNS) {
    "Columns must be a non-empty range within 0..8, but were $columns"
}

internal fun requirePlayerSlot(
    column: Int,
    row: Int,
) {
    require(column in 0 until PlayerInventoryLayout.COLUMNS) { "Player inventory column must be within 0..8, but was $column" }
    requirePlayerRow(row)
}

internal fun requirePlayerRow(row: Int) =
    require(row in 0 until PlayerInventoryLayout.ROWS) { "Player inventory row must be within 0..3, but was $row" }
