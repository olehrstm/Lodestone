package de.ole101.lodestone.menu.session

import de.ole101.lodestone.menu.CloseReason
import de.ole101.lodestone.menu.MenuSession
import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * The way back of each player, and the players who may come back to a session or answer a prompt outside it.
 */
internal class Navigation(
    private val session: MenuSession,
) {
    // The session each player goes back to from here, and the players who may come back here from a later menu
    private val backs = ConcurrentHashMap<Player, MenuSession>()
    private val away = ConcurrentHashMap.newKeySet<Player>()
    private val prompts = ConcurrentHashMap<Player, PromptToken>()

    val hasNoAway: Boolean
        get() = away.isEmpty()

    fun isAway(player: Player): Boolean = player in away

    fun canGoBack(player: Player): Boolean = backs[player]?.isDisposed == false

    fun open(
        player: Player,
        back: MenuSession,
    ) {
        val backNavigation = back.navigation
        // The player may already be away from back, for example when this session is opened twice
        val alreadyAway = !backNavigation.away.add(player)
        if (!session.openViewer(player)) {
            if (!alreadyAway) {
                backNavigation.away -= player
                back.disposeIfUnused()
            }
            return
        }

        val previous = backs.put(player, back)
        if (previous != null && previous !== back) previous.navigation.release(player)
        back.removeViewer(player, CloseReason.NAVIGATED)
    }

    fun goBack(player: Player): Boolean {
        val back = backs[player] ?: return false
        if (back.isDisposed) {
            backs -= player
            return false
        }

        val backNavigation = back.navigation
        backNavigation.away -= player
        if (!back.openViewer(player)) {
            backNavigation.away += player
            return false
        }

        backs -= player
        // Going back leaves this session for good, so the player's items come back
        session.inputSlots.returnItemsOf(player)
        session.removeViewer(player, CloseReason.NAVIGATED)
        return true
    }

    // The player will not come back here, so neither to the sessions before this one
    fun release(player: Player) {
        away -= player
        releaseBack(player)
        session.disposeIfUnused()
    }

    fun releaseBack(player: Player) {
        backs.remove(player)?.navigation?.release(player)
    }

    fun releaseAllBacks() = backs.keys.toList().forEach(::releaseBack)

    /**
     * Keeps the session for [player] while they answer a prompt outside it, and closes it for them if they view it.
     * Returns the token that [endPrompt] needs.
     */
    fun beginPrompt(player: Player): PromptToken {
        session.checkNotDisposed()
        val token = PromptToken()
        prompts[player] = token
        away += player
        if (player.openInventory === session.inventory) player.closeInventory()
        return token
    }

    // Ends the prompt without opening the session again, because the player left it for good
    fun abandonPrompt(
        player: Player,
        token: PromptToken,
    ) {
        if (prompts.remove(player, token)) release(player)
    }

    // Opens the session again, unless a newer prompt started, the player went offline or opened another inventory
    fun endPrompt(
        player: Player,
        token: PromptToken,
    ) {
        if (!prompts.remove(player, token)) return

        away -= player
        if (!session.isDisposed && player.isOnline && player.openInventory == null) session.openViewer(player)
        session.disposeIfUnused()
    }

    // A player who leaves the server never answers their prompt
    fun endOfflinePrompts() {
        prompts
            .filterKeys { !it.isOnline }
            .forEach { (player, token) -> endPrompt(player, token) }
    }
}

// A token per prompt, so an older prompt cannot end a newer one
internal class PromptToken
