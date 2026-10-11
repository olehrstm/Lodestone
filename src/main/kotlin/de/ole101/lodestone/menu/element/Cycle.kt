package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.reactive.node.State
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack

/**
 * Adds a slot that switches between [options] when clicked. A left click selects the next option and a right click the
 * previous one, wrapping around at the ends. Shift clicks count the same. [display] returns the item shown for the
 * selected option and is called again when a state it read changes. Returns the [Cycler] that holds the selection.
 *
 * @throws IllegalArgumentException if [options] is empty, [initial] is not one of them, or [slot] is outside the menu.
 */
public fun <T> MenuScope.cycle(
    slot: Int,
    options: List<T>,
    initial: T = firstOption(options),
    display: (T) -> ItemStack,
): Cycler<T> {
    require(initial in options) { "Initial option $initial is not one of $options" }
    val cycler = Cycler(options.toList(), state(options.indexOf(initial)))

    slot(slot) {
        item { display(cycler.selected) }
        onClick { click ->
            when (click.click) {
                is Click.Left, is Click.LeftShift -> {
                    cycler.next()
                }

                is Click.Right, is Click.RightShift -> {
                    cycler.previous()
                }

                else -> {}
            }
        }
    }
    return cycler
}

public fun <T> MenuScope.cycle(
    column: Int,
    row: Int,
    options: List<T>,
    initial: T = firstOption(options),
    display: (T) -> ItemStack,
): Cycler<T> = cycle(session.type.slotIndex(column, row), options, initial, display)

private fun <T> firstOption(options: List<T>): T {
    require(options.isNotEmpty()) { "Cycle needs at least one option" }
    return options.first()
}

/**
 * The selection of a [cycle] slot. Reading [selected] in an effect or element tracks it.
 */
public class Cycler<T> internal constructor(
    public val options: List<T>,
    private val index: State<Int>,
) {
    /**
     * The selected option.
     *
     * @throws IllegalArgumentException when set to a value that is not one of [options].
     */
    public var selected: T
        get() = options[index.value]
        set(value) {
            val newIndex = options.indexOf(value)
            require(newIndex >= 0) { "Option $value is not one of $options" }
            index.value = newIndex
        }

    /**
     * Selects the next option, or the first after the last.
     */
    public fun next(): Unit = step(1)

    /**
     * Selects the previous option, or the last before the first.
     */
    public fun previous(): Unit = step(-1)

    private fun step(delta: Int) {
        index.value = (index.value + delta).mod(options.size)
    }
}
