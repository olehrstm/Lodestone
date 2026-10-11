package de.ole101.lodestone.menu.slot

import net.minestom.server.entity.Player
import net.minestom.server.inventory.click.Click

/**
 * A click of [player] on [slot] of a menu, or of the player's own inventory for `onPlayerInventoryClick`.
 * [click] tells which button and keys were used.
 */
public class ClickContext internal constructor(
    public val player: Player,
    public val slot: Int,
    public val click: Click,
    /**
     * Whether the click is cancelled, so it moves no items. It starts with what the slot policy allows, and handlers
     * may change it, for example to take an item from the player's inventory without it moving to the cursor.
     */
    public var isCancelled: Boolean,
)
