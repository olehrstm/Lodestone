package de.ole101.lodestone.menu.session

import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.slot.ClickContext
import de.ole101.lodestone.menu.slot.allows
import de.ole101.lodestone.menu.slot.isInput
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack

/**
 * Decides which clicks of viewers in a session and in their own inventory below it may move items.
 */
internal class ClickHandler(
    private val session: MenuSession,
) {
    val cooldown = ClickCooldown()

    private val inventory
        get() = session.inventory
    private val hooks
        get() = session.hooks

    fun handle(event: InventoryPreClickEvent) {
        val player = event.player
        // Minestom sends the real items again after a click, so the player slots are shown again
        if (session.playerOverlay.isShownTo(player)) session.playerOverlay.showAtTickEnd(player)

        when {
            event.inventory === inventory -> handleMenuClick(event)
            player.openInventory === inventory -> handlePlayerInventoryClick(event)
        }
    }

    private fun handleMenuClick(event: InventoryPreClickEvent) {
        val cursor = event.player.inventory.cursorItem
        val slot = event.click.slot()
        when {
            cooldown.isCoolingDown(event.player) -> event.isCancelled = true
            handleMultiSlotClick(event, cursor) -> Unit
            slot !in 0 until inventory.size -> event.isCancelled = true
            else -> handleSlotClick(event, slot, cursor)
        }
    }

    private fun handleSlotClick(
        event: InventoryPreClickEvent,
        slot: Int,
        cursor: ItemStack,
    ) {
        val player = event.player
        val click = event.click
        val view = session.frame.slots[slot]
        val swapped = swappedItem(player, click)
        val allowed = view != null && view.policy.allows(click, inventory.getItemStack(slot), cursor, swapped, view.accepts)

        val context = ClickContext(player, slot, click, isCancelled = !allowed)
        hooks.click.forEach { it(context) }
        view?.onClick?.invoke(context)
        event.isCancelled = context.isCancelled
        if (!context.isCancelled && view.isInput) session.inputSlots.claim(slot, player)
    }

    // Drags and double clicks need policies for every slot they may touch.
    private fun handleMultiSlotClick(
        event: InventoryPreClickEvent,
        cursor: ItemStack,
    ): Boolean = when (val click = event.click) {
        is Click.Drag -> {
            val menuSlots = click.slots().filter { it < inventory.size }
            val allowed = click !is Click.MiddleDrag && menuSlots.all { session.inputSlots.canPlace(it, cursor) }
            event.isCancelled = !allowed
            if (allowed) menuSlots.forEach { session.inputSlots.claim(it, event.player) }
            true
        }

        is Click.Double -> {
            event.isCancelled = !session.inputSlots.canCollect(cursor)
            true
        }

        else -> {
            false
        }
    }

    private fun handlePlayerInventoryClick(event: InventoryPreClickEvent) {
        val click = event.click
        // The client reports clicks outside the GUI as dropping the cursor, also when the cursor is empty
        if (click is Click.DropCursor) return handleOutsideClick(event, click)

        val covered = session.frame.playerSlots
        val coveredIndex = PlayerSlotOverlay.indexOf(click.slot())?.takeIf { it in covered }
        when {
            // Drags and double clicks may touch slots the menu shows instead of the player's items
            covered.isNotEmpty() && (click is Click.Drag || click is Click.Double) -> {
                event.isCancelled = true
            }

            coveredIndex != null -> {
                val view = covered.getValue(coveredIndex)
                view.onClick?.invoke(ClickContext(event.player, coveredIndex, click, isCancelled = true))
                event.isCancelled = true
            }

            else -> {
                handleOwnInventoryClick(event)
            }
        }
    }

    private fun handleOutsideClick(
        event: InventoryPreClickEvent,
        click: Click.DropCursor,
    ) {
        val context = ClickContext(event.player, click.slot(), click, isCancelled = false)
        hooks.outsideClick.forEach { it(context) }
        event.isCancelled = context.isCancelled
    }

    private fun handleOwnInventoryClick(event: InventoryPreClickEvent) {
        val player = event.player
        val click = event.click
        // Double clicks also collect from the menu, so they must not take items the menu owns
        val takesMenuItems = click is Click.Double && !session.inputSlots.canCollect(player.inventory.cursorItem)
        val context = ClickContext(player, click.slot(), click, isCancelled = takesMenuItems)
        hooks.playerInventoryClick.forEach { it(context) }

        val isShiftClick = click is Click.LeftShift || click is Click.RightShift
        // Minestom would fill any menu slot, so only input slots are filled here
        if (isShiftClick && !context.isCancelled) session.inputSlots.moveInto(player, click.slot())
        event.isCancelled = context.isCancelled || isShiftClick
    }

    private fun swappedItem(
        player: Player,
        click: Click,
    ): ItemStack = when (click) {
        is Click.HotbarSwap -> player.inventory.getItemStack(click.hotbarSlot())
        is Click.OffhandSwap -> player.getEquipment(EquipmentSlot.OFF_HAND)
        else -> ItemStack.AIR
    }
}
