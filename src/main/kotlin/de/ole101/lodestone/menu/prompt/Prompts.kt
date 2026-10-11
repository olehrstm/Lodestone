package de.ole101.lodestone.menu.prompt

import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.sign.SignInputBuilder
import de.ole101.lodestone.sign.signInput
import net.minestom.server.entity.Player

/**
 * Closes this session for [player], asks for text on a sign configured in [block] (see [SignInputBuilder]), runs its
 * `onInput` handler and opens this session again, with its state as the player left it. The session stays alive while
 * the player types, even with `DisposePolicy.WHEN_EMPTY`, and the player's items stay in its input slots.
 *
 * The session is not opened again if the handler prompts again or opens another inventory, for example another menu.
 * A player who leaves the server releases the session. If this throws, the session is opened again.
 *
 * @throws IllegalStateException if the session is disposed, [block] sets no `onInput` handler or the player is in no
 * instance.
 * @throws IllegalArgumentException if [block] sets invalid lines.
 */
public fun MenuSession.promptSign(
    player: Player,
    block: SignInputBuilder.() -> Unit,
) {
    val input = signInput(block)
    val token = navigation.beginPrompt(player)
    try {
        input.open(
            player,
            onClosed = { navigation.endPrompt(player, token) },
            // Another sign replaced this one, so its handler never runs and the player does not come back
            onReplaced = { navigation.abandonPrompt(player, token) },
        )
    } catch (exception: Throwable) {
        navigation.endPrompt(player, token)
        throw exception
    }
}
