package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuDsl
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.layout.cellOf
import de.ole101.lodestone.reactive.State
import de.ole101.lodestone.text.glyph.Glyph

/**
 * Adds the tabs declared in [block], starting with [initial] active. A click on any slot of a tab activates it.
 * Tab glyphs are centered over their slots, also vertically when they have a [Glyph.height].
 * Show the content of the active tab with `key({ tabs.active }) { tab -> ... }`.
 * Throws [IllegalArgumentException] if a tab slot is outside the menu or a glyph has no width.
 */
public fun <K> MenuScope.tabs(initial: K, block: TabsScope<K>.() -> Unit): Tabs<K> {
    val declared = TabsScope<K>(session.type).apply(block).tabs
    val tabs = Tabs(state(initial))
    val slots = declared.map { tab -> spanSlots(tab.columns, tab.row) }
    val invisible = invisibleItem(null)

    element {
        declared.forEachIndexed { index, tab ->
            spanGlyph(if (tabs.active == tab.key) tab.activeGlyph else tab.glyph, tab.columns, tab.row)
            for (slot in slots[index]) {
                slot(slot) {
                    item { invisible }
                    onClick { tabs.active = tab.key }
                }
            }
        }
    }
    return tabs
}

public class Tabs<K> internal constructor(active: State<K>) {
    /** The key of the active tab. Reading it in an effect or element tracks it. */
    public var active: K by active
}

@MenuDsl
public class TabsScope<K> internal constructor(private val type: MenuType) {
    internal val tabs = mutableListOf<Tab<K>>()

    /**
     * Adds a tab for [key], drawn as [glyph] over [slot], or as [activeGlyph] while it is active.
     * Throws [IllegalArgumentException] if [slot] is outside the menu.
     */
    public fun tab(key: K, slot: Int, glyph: Glyph, activeGlyph: Glyph = glyph) {
        val (column, row) = type.cellOf(slot)
        tab(key, column..column, row, glyph, activeGlyph)
    }

    public fun tab(key: K, column: Int, row: Int, glyph: Glyph, activeGlyph: Glyph = glyph) {
        tab(key, column..column, row, glyph, activeGlyph)
    }

    /** Adds a tab for [key] that spans the slots in [columns] of [row], for glyphs wider than one slot. */
    public fun tab(key: K, columns: IntRange, row: Int, glyph: Glyph, activeGlyph: Glyph = glyph) {
        tabs += Tab(key, glyph, activeGlyph, columns, row)
    }

    internal class Tab<K>(val key: K, val glyph: Glyph, val activeGlyph: Glyph, val columns: IntRange, val row: Int)
}
