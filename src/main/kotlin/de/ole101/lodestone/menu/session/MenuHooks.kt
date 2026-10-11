package de.ole101.lodestone.menu.session

import de.ole101.lodestone.menu.CloseReason
import de.ole101.lodestone.menu.render.Frame
import de.ole101.lodestone.menu.slot.ClickContext
import net.minestom.server.entity.Player
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The hooks that menu blocks registered on a session.
 */
internal class MenuHooks {
    val open = CopyOnWriteArrayList<(Player) -> Unit>()
    val close = CopyOnWriteArrayList<(Player, CloseReason) -> Unit>()
    val click = CopyOnWriteArrayList<(ClickContext) -> Unit>()
    val playerInventoryClick = CopyOnWriteArrayList<(ClickContext) -> Unit>()
    val outsideClick = CopyOnWriteArrayList<(ClickContext) -> Unit>()
    val tick = CopyOnWriteArrayList<() -> Unit>()

    // Session features such as the anvil field hook in here instead of the session calling them
    val beforeWindowOpen = CopyOnWriteArrayList<() -> Unit>()
    val afterSlotsApplied = CopyOnWriteArrayList<(previous: Frame) -> Unit>()
}
