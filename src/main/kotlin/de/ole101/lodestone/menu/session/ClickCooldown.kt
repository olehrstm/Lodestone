package de.ole101.lodestone.menu.session

import net.minestom.server.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

internal class ClickCooldown {
    var duration: Duration = Duration.ZERO

    private val lastClicks = ConcurrentHashMap<UUID, Long>()

    /**
     * Returns whether [player] clicked within [duration] of their last counted click. Otherwise it counts this click
     * and returns false, so a cancelled click does not extend the cooldown.
     */
    fun isCoolingDown(player: Player): Boolean {
        if (!duration.isPositive()) return false

        val now = System.nanoTime()
        val last = lastClicks[player.uuid]
        if (last != null && now - last < duration.inWholeNanoseconds) return true

        lastClicks[player.uuid] = now
        return false
    }

    fun forget(player: Player) {
        lastClicks -= player.uuid
    }
}
