package de.ole101.lodestone.menu.session

import de.ole101.lodestone.event.eventNode
import de.ole101.lodestone.event.listen
import de.ole101.lodestone.menu.MenuSession
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.tag.Tag
import java.util.concurrent.ConcurrentHashMap

/**
 * Routes the events of player inventories to the sessions they belong to. Events of a menu's own inventory go to the
 * inventory's event node instead, so no session sees events of other inventories.
 */
internal object MenuEvents {
    private val SESSION: Tag<MenuSession> = Tag.Transient("lodestone:menu-session")
    private val sessions = ConcurrentHashMap.newKeySet<MenuSession>()

    init {
        val node = eventNode("lodestone:menu")
        listen<InventoryPreClickEvent>(node, ::handlePlayerInventoryClick)
        listen<InventoryItemChangeEvent>(node, ::handlePlayerInventoryChange)
        listen<PlayerDisconnectEvent>(node, ::handleDisconnect)
    }

    fun register(session: MenuSession) {
        session.inventory.setTag(SESSION, session)
        sessions += session
    }

    fun unregister(session: MenuSession) {
        sessions -= session
    }

    private fun handlePlayerInventoryClick(event: InventoryPreClickEvent) {
        if (event.inventory !is PlayerInventory) return
        val session = event.player.openInventory?.getTag(SESSION) ?: return
        session.clicks.handlePlayerInventoryClick(event)
    }

    private fun handlePlayerInventoryChange(event: InventoryItemChangeEvent) {
        val inventory = event.inventory as? PlayerInventory ?: return
        val session = PlayerSlotOverlay.ownerOf(inventory) ?: return
        session.playerOverlay.showAgainAfterChange(inventory)
    }

    // A player may have items in a session they navigated away from, so every live session is checked
    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        sessions.forEach { it.inputSlots.returnItemsOf(event.player) }
    }
}
