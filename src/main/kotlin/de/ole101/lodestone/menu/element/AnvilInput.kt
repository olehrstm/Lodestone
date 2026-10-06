package de.ole101.lodestone.menu.element

import de.ole101.lodestone.item.item
import de.ole101.lodestone.menu.*
import de.ole101.lodestone.menu.layout.MenuType
import net.kyori.adventure.text.Component
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * Creates an anvil input blueprint with [initial] text and actions configured by [block]. Each open creates its own
 * input session. See [AnvilInputScope] for confirmation, validation, and custom artwork.
 * Throws [IllegalStateException] if [block] registers no confirmation handler. The block runs on the tick thread
 * during session creation, and its exceptions reach the caller.
 */
public fun anvilInput(initial: String = "", block: AnvilInputScope.() -> Unit): MenuBlueprint<Unit> =
    menu(MenuType.ANVIL) { anvilInput(initial, block) }

/**
 * Configures this anvil's rename field with [initial] text, which [MenuSession.anvilInput] reports filtered and trimmed, a cancel action in slot 0, and confirmation in slot 2.
 * Slot 1 is left empty. Actions react to left clicks only and never move items or consume experience. Cancelling or
 * successfully confirming goes back when possible, otherwise closes the inventory. The handler runs after that, so it
 * can open another menu.
 * Throws [IllegalStateException] if this is not an anvil, input was already configured, or no confirmation handler
 * was registered. [block] runs during setup on the tick thread, and its exceptions reach the caller.
 */
public fun MenuScope.anvilInput(initial: String = "", block: AnvilInputScope.() -> Unit) {
    check(session.type == MenuType.ANVIL) { "Anvil input requires ANVIL, but was ${session.type}" }
    val input = AnvilInputScope().apply(block)
    val confirm = checkNotNull(input.confirm) { "Anvil input needs an onConfirm handler" }
    session.initializeAnvilInput(initial)
    input.layout?.invoke(this)

    slot(0) {
        // A negative repair cost makes the client predict a cost of 0 while typing, so it shows no cost label
        item { input.cancelItem().withCustomName(Component.text(initial)).with(DataComponents.REPAIR_COST, NO_COST) }
        onClick { click ->
            if (click.click !is Click.Left) return@onClick
            session.finishAnvilInput(click.player)
            input.cancel?.invoke(click.player)
        }
    }
    slot(2) {
        item { input.confirmItem() }
        onClick { click ->
            if (click.click !is Click.Left) return@onClick
            val text = session.anvilInput
            if (!input.accepts(text)) return@onClick
            session.finishAnvilInput(click.player)
            confirm(click.player, text)
        }
    }
}

private const val NO_COST = -1_000_000

// Leaves the input before its handler runs, so the handler can open another menu
private fun MenuSession.finishAnvilInput(player: Player) {
    if (!goBack(player)) player.closeInventory()
}

/** Configures an anvil input's actions and appearance. */
@MenuDsl
public class AnvilInputScope internal constructor() {
    internal var confirm: ((Player, String) -> Unit)? = null
    internal var cancel: ((Player) -> Unit)? = null
    internal var accepts: (String) -> Boolean = { true }
    internal var layout: (MenuScope.() -> Unit)? = null
    internal var cancelItem: () -> ItemStack = {
        item(Material.ARROW) { noTooltip() }
    }
    internal var confirmItem: () -> ItemStack = {
        item(Material.LIME_DYE) { customName(Component.text("Confirm")) }
    }

    /**
     * Runs [handler] with the latest text when confirmation is clicked and validation passes, after the input was left.
     * Runs on the clicking player's tick thread. Exceptions reach the event dispatcher.
     */
    public fun onConfirm(handler: (Player, String) -> Unit) {
        confirm = handler
    }

    /**
     * Runs [handler] when the left action is clicked, after going back or closing. Runs on the clicking player's
     * tick thread. Exceptions reach the event dispatcher. Closing with Escape is not a cancel click.
     */
    public fun onCancel(handler: (Player) -> Unit) {
        cancel = handler
    }

    /**
     * Allows confirmation only when [predicate] accepts the text. All text is accepted by default, including empty
     * text. Runs on the clicking player's tick thread. Exceptions reach the event dispatcher and leave the input open.
     */
    public fun accepts(predicate: (String) -> Boolean) {
        accepts = predicate
    }

    /**
     * Draws custom artwork and a title using [block], for example `overlay(background, px(0, 0))` and sprites over
     * slots 0 and 2. Requires matching resource-pack assets and the GUI text shader for overlays. The native field
     * stays at its vanilla position. Runs during setup on the tick thread, and its exceptions reach the caller.
     */
    public fun layout(block: MenuScope.() -> Unit) {
        layout = block
    }

    /**
     * Uses the item returned by [block] for the left action. Its custom name is replaced with the input text because
     * the client seeds the rename field from it. Must return a non-air item to enable typing. Runs on the tick thread
     * during rendering. Exceptions, including [IllegalArgumentException] for air items, go to the exception manager.
     */
    public fun cancelItem(block: () -> ItemStack) {
        cancelItem = {
            block().also { require(!it.isAir) { "Anvil cancel item must not be air" } }
        }
    }

    /**
     * Uses the item returned by [block] for the right action. Must return a non-air item to provide a clickable result.
     * Runs on the tick thread during rendering. Exceptions, including [IllegalArgumentException] for air items,
     * go to the exception manager.
     */
    public fun confirmItem(block: () -> ItemStack) {
        confirmItem = {
            block().also { require(!it.isAir) { "Anvil confirm item must not be air" } }
        }
    }
}
