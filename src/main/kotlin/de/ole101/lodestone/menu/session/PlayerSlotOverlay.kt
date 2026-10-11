package de.ole101.lodestone.menu.session

import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.layout.PlayerInventoryLayout
import net.minestom.server.entity.Player
import net.minestom.server.inventory.AbstractInventory
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.WindowItemsPacket
import net.minestom.server.tag.Tag
import net.minestom.server.utils.inventory.PlayerInventoryUtils

internal class PlayerSlotOverlay(
    private val session: MenuSession,
) {
    fun isShownTo(player: Player): Boolean = player.getTag(OWNER) === session

    /**
     * Returns the window items that show [player] their inventory with this menu's player slots on top.
     */
    fun windowItems(player: Player): List<ItemStack> {
        val items = MutableList(PlayerInventory.INVENTORY_SIZE) { ItemStack.AIR }

        fun put(
            inventorySlot: Int,
            item: ItemStack,
        ) {
            items[PlayerInventoryUtils.convertMinestomSlotToWindowSlot(inventorySlot)] = item
        }

        repeat(PlayerInventory.INVENTORY_SIZE) { put(it, player.inventory.getItemStack(it)) }
        session.frame.playerSlots.forEach { (index, view) -> put(inventorySlot(index), view.item) }
        return items
    }

    // The client shows the player inventory window behind every container, so this replaces what it shows
    fun show(player: Player) {
        if (session.frame.playerSlots.isEmpty()) return restore(player)

        player.setTag(OWNER, session)
        player.sendPacket(WindowItemsPacket(0, 0, windowItems(player), player.inventory.cursorItem))
    }

    fun showAtTickEnd(player: Player) {
        player.scheduler().scheduleEndOfTick {
            if (isShownTo(player) && player in session.currentViewers) show(player)
        }
    }

    // Minestom sends the real item when a slot of a player inventory changes, so the player slots are shown again
    fun showAgainAfterChange(playerInventory: AbstractInventory) {
        val owner = session.currentViewers.firstOrNull { it.inventory === playerInventory } ?: return
        if (isShownTo(owner)) showAtTickEnd(owner)
    }

    // Only the session that showed its player slots restores them, so a menu opened over it keeps its own
    fun restore(player: Player) {
        if (!isShownTo(player)) return

        player.removeTag(OWNER)
        if (player.isOnline) player.inventory.update()
    }

    companion object {
        // The session whose player slots a player's client shows instead of their own items
        private val OWNER: Tag<MenuSession> = Tag.Transient("lodestone:menu-player-slots")

        fun indexOf(inventorySlot: Int): Int? = when (inventorySlot) {
            in PlayerInventoryLayout.COLUMNS..PlayerInventoryLayout.LAST_SLOT -> {
                inventorySlot - PlayerInventoryLayout.COLUMNS
            }

            in 0 until PlayerInventoryLayout.COLUMNS -> {
                inventorySlot + PlayerInventoryLayout.MAIN_SLOTS
            }

            else -> {
                null
            }
        }

        // Player slots count row by row through the main inventory, with the hotbar as the last row
        private fun inventorySlot(index: Int): Int = if (index < PlayerInventoryLayout.MAIN_SLOTS) {
            index + PlayerInventoryLayout.COLUMNS
        } else {
            index - PlayerInventoryLayout.MAIN_SLOTS
        }
    }
}
