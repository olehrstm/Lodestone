package de.ole101.lodestone.menu.prompt

import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.sign.SignInput
import de.ole101.lodestone.sign.SignInputBuilder
import net.minestom.server.entity.Player

/**
 * Closes this session for [player], asks for text on a sign configured in [block] (see [SignInputBuilder]), runs its
 * `onInput` handler and opens this session again, with its state as the player left it. The session stays alive while
 * the player types, even with `DisposePolicy.WHEN_EMPTY`, and the player's items stay in its input slots.
 *
 * The session is not opened again if the handler prompts again or opens another inventory, for example another menu.
 * A player who leaves the server releases the session. Throws [IllegalStateException] if the session is disposed,
 * [block] sets no `onInput` handler or the player is in no instance, and [IllegalArgumentException] for invalid lines.
 * The session is then opened again.
 */
public fun MenuSession.promptSign(player: Player, block: SignInputBuilder.() -> Unit) {
    val input = SignInputBuilder().apply(block).build()
    val token = beginPrompt(player)
    val sign = SignInput(input.lines, input.inputLines) { signPlayer, text ->
        try {
            input.onInput(signPlayer, text)
        } finally {
            endPrompt(player, token)
        }
    }
    try {
        sign.open(player)
    } catch (e: Throwable) {
        endPrompt(player, token)
        throw e
    }
}
