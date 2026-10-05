package de.ole101.lodestone.menu.layout

/**
 * Where text or a glyph is drawn in a menu, either at a pixel with [px] or in the slot grid with [cell].
 */
public sealed interface Position {
    public class Pixel internal constructor(public val x: Int, public val y: Int) : Position

    public class Cell internal constructor(public val columns: IntRange, public val row: Int) : Position
}

/** Returns the point [x], [y] in GUI pixels from the top-left corner of the menu. */
public fun px(x: Int, y: Int): Position = Position.Pixel(x, y)

/**
 * Returns the slot at [column] and [row], both counted from 0.
 * Throws [IllegalArgumentException] if [column] is outside `0..8` or [row] is negative.
 */
public fun cell(column: Int, row: Int): Position = cell(column..column, row)

/**
 * Returns the slots in [columns] of [row], for example `cell(0..8, 5)` for the whole sixth row.
 * Throws [IllegalArgumentException] if [columns] is empty or outside `0..8`, or [row] is negative.
 */
public fun cell(columns: IntRange, row: Int): Position {
    require(!columns.isEmpty() && columns.first >= 0 && columns.last <= 8) { "Columns must be a non-empty range within 0..8, but were $columns" }
    require(row >= 0) { "Row must not be negative, but was $row" }
    return Position.Cell(columns, row)
}

public enum class VAlign {
    TOP,
    MIDDLE,
    BOTTOM
}
