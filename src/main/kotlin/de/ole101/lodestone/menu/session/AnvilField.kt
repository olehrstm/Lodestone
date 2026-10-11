package de.ole101.lodestone.menu.session

import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.reactive.node.state
import net.kyori.adventure.text.Component
import net.minestom.server.event.player.PlayerAnvilInputEvent

/**
 * The rename field of an anvil session.
 */
internal class AnvilField(
    private val session: MenuSession,
) {
    // The text as the client shows it, so seeding slot 0 does not change what the player typed. Null until configured.
    private var typed: String? = null

    var text: String by state("")
        private set

    fun configure(initial: String) {
        check(session.type == MenuType.ANVIL) { "Anvil input requires ANVIL, but was ${session.type}" }
        check(typed == null) { "Anvil input is already configured" }
        typed = initial
        text = sanitize(initial)
    }

    fun handleInput(event: PlayerAnvilInputEvent) {
        if (!isInputForThisAnvil(event)) return

        typed = event.input
        val sanitized = sanitize(event.input)
        if (sanitized != text) text = sanitized
        // The client predicts a rename result while typing, so the menu's result item is sent again
        session.inventory.sendSlotRefresh(2, session.inventory.getItemStack(2))
    }

    // The client resets the rename text to the name of slot 0 when that item changes or the window reopens
    fun seedIfConfigured() {
        val typed = typed ?: return
        val inventory = session.inventory
        session.withoutChangeEvents {
            inventory.setItemStack(0, inventory.getItemStack(0).withCustomName(Component.text(typed)))
        }
    }

    private fun isInputForThisAnvil(event: PlayerAnvilInputEvent): Boolean = session.type == MenuType.ANVIL &&
        event.inventory === session.inventory &&
        event.player in session.currentViewers

    private fun sanitize(text: String) = text.filter { it in PRINTABLE_ASCII }.trim()
}

private val PRINTABLE_ASCII = ' '..'~'
