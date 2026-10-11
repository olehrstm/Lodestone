package de.ole101.lodestone.menu.session

import net.minestom.server.entity.Player
import java.util.UUID
import kotlin.time.Duration

internal class ClickCooldown {
    var duration: Duration = Duration.ZERO

    private val lastClicks = HashMap<UUID, Long>()

    /**
     * Counts this click of [player] and returns true, unless they clicked within [duration] of their last counted
     * click. A rejected click is not counted, so it does not extend the cooldown.
     */
    fun tryAcceptClick(player: Player): Boolean {
        if (!duration.isPositive()) return true

        val now = System.nanoTime()
        val last = lastClicks[player.uuid]
        if (last != null && now - last < duration.inWholeNanoseconds) return false

        lastClicks[player.uuid] = now
        return true
    }

    fun forget(player: Player) {
        lastClicks -= player.uuid
    }
}
