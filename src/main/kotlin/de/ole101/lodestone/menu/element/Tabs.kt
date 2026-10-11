package de.ole101.lodestone.menu.element

import de.ole101.lodestone.LodestoneDsl
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.layout.cell
import de.ole101.lodestone.menu.render.requireWidth
import de.ole101.lodestone.reactive.node.State
import de.ole101.lodestone.text.glyph.Glyph

/**
 * Adds the tabs declared in [block], starting with [initial] active. A click on any slot of a tab activates it.
 * Tab glyphs are centered over their slots, also vertically when they have a [Glyph.height].
 * Show the content of the active tab with `key({ tabs.active }) { tab -> ... }`.
 *
 * @throws IllegalArgumentException if a tab slot is outside the menu or a glyph has no width.
 */
public fun <K> MenuScope.tabs(
    initial: K,
    block: TabsScope<K>.() -> Unit,
): Tabs<K> {
    val declared = TabsScope<K>(session.type).apply(block).tabs
    val tabs = Tabs(state(initial))
    val invisible = invisibleItem(null)
    val slotsOf = declared.associateWith { spanSlots(it.columns, it.row) }

    element {
        for (tab in declared) {
            spanGlyph(if (tabs.active == tab.key) tab.activeGlyph else tab.glyph, cell(tab.columns, tab.row))
            for (index in slotsOf.getValue(tab)) {
                slot(index) {
                    item { invisible }
                    onClick { tabs.active = tab.key }
                }
            }
        }
    }
    return tabs
}

public class Tabs<K> internal constructor(
    active: State<K>,
) {
    /**
     * The key of the active tab. Reading it in an effect or element tracks it.
     */
    public var active: K by active
}

@LodestoneDsl
public class TabsScope<K> internal constructor(
    private val type: MenuType,
) {
    internal val tabs = mutableListOf<Tab<K>>()

    /**
     * Adds a tab for [key], drawn as [glyph] over [slot], or as [activeGlyph] while it is active.
     *
     * @throws IllegalArgumentException if [slot] is outside the menu or a glyph has no width.
     */
    public fun tab(
        key: K,
        slot: Int,
        glyph: Glyph,
        activeGlyph: Glyph = glyph,
    ) {
        val (column, row) = type.cellOf(slot)
        tab(key, column, row, glyph, activeGlyph)
    }

    public fun tab(
        key: K,
        column: Int,
        row: Int,
        glyph: Glyph,
        activeGlyph: Glyph = glyph,
    ) {
        tab(key, column..column, row, glyph, activeGlyph)
    }

    /**
     * Adds a tab for [key] that spans the slots in [columns] of [row], for glyphs wider than one slot.
     */
    public fun tab(
        key: K,
        columns: IntRange,
        row: Int,
        glyph: Glyph,
        activeGlyph: Glyph = glyph,
    ) {
        glyph.requireWidth()
        activeGlyph.requireWidth()
        tabs += Tab(key, glyph, activeGlyph, columns, row)
    }

    internal class Tab<K>(
        val key: K,
        val glyph: Glyph,
        val activeGlyph: Glyph,
        val columns: IntRange,
        val row: Int,
    )
}
