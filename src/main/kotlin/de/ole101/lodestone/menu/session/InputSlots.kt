package de.ole101.lodestone.menu.session

import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.slot.allowsCollecting
import de.ole101.lodestone.menu.slot.allowsPlacing
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

/**
 * Tracks who put items into the input slots of a session and gives them back.
 */
internal class InputSlots(
    private val session: MenuSession,
) {
    // Who last put items into each input slot, to give them back. A stack filled by several players goes to the last one
    private val owners = HashMap<Int, Player>()

    private val inventory
        get() = session.inventory

    fun claim(
        slot: Int,
        player: Player,
    ) {
        owners[slot] = player
    }

    fun forget(slot: Int) {
        owners -= slot
    }

    fun canPlace(
        slot: Int,
        item: ItemStack,
    ): Boolean {
        val view = session.frame.slots[slot] ?: return false
        return view.policy.allowsPlacing(session.inventory.getItemStack(slot), item, view.accepts)
    }

    fun canCollect(cursor: ItemStack): Boolean = (0 until session.inventory.size).all { slot ->
        session.frame.slots[slot]
            ?.policy
            .allowsCollecting(session.inventory.getItemStack(slot), cursor)
    }

    // Stacks onto matching input slots first, then fills empty ones, like a shift click into a chest
    fun moveInto(
        player: Player,
        playerSlot: Int,
    ) {
        var remaining = player.inventory.getItemStack(playerSlot)
        if (remaining.isAir) return

        val targets = (0 until inventory.size)
            .filter { canPlace(it, remaining) }
            .sortedBy { inventory.getItemStack(it).isAir }
        for (slot in targets) {
            if (remaining.isAir) break
            remaining = stackInto(player, slot, remaining)
        }
        player.inventory.setItemStack(playerSlot, remaining)
    }

    fun returnItemsOf(player: Player) = owners.filterValues { it == player }.keys.forEach(::returnItem)

    fun returnAll() {
        owners.keys.toList().forEach(::returnItem)
    }

    // Gives the item in slot to the player who put it there. A player who left still gets it into their inventory,
    // and only an online player can drop what does not fit.
    fun returnItem(slot: Int) {
        val owner = owners[slot] ?: return
        owners -= slot

        val item = inventory.getItemStack(slot)
        if (item.isAir) return
        session.withoutChangeEvents { inventory.setItemStack(slot, ItemStack.AIR) }
        session.markContentsChanged()
        if (!owner.inventory.addItemStack(item) && owner.isOnline) owner.dropItem(item)
    }

    // Returns what did not fit
    private fun stackInto(
        player: Player,
        slot: Int,
        remaining: ItemStack,
    ): ItemStack {
        val current = inventory.getItemStack(slot)
        val stacked = if (current.isAir) 0 else current.amount()
        val moved = minOf(remaining.amount(), remaining.maxStackSize() - stacked)
        if (moved <= 0) return remaining

        inventory.setItemStack(slot, remaining.withAmount(stacked + moved))
        claim(slot, player)
        return if (remaining.amount() == moved) ItemStack.AIR else remaining.withAmount(remaining.amount() - moved)
    }
}
