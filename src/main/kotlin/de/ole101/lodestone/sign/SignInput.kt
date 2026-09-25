package de.ole101.lodestone.sign

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.EventListener
import net.minestom.server.event.player.PlayerEditSignEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockEntityType
import net.minestom.server.network.packet.server.play.BlockChangePacket
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket
import net.minestom.server.tag.Tag
import net.minestom.server.utils.block.BlockUtils

/**
 * Builds a [SignInput] that asks players for text through the sign editor. Call [SignInputBuilder.onInput] in
 * [block], or this throws [IllegalStateException]. Throws [IllegalArgumentException] for invalid lines.
 */
public fun signInput(block: SignInputBuilder.() -> Unit): SignInput = SignInputBuilder().apply(block).build()

@DslMarker
public annotation class SignInputDsl

@SignInputDsl
public class SignInputBuilder internal constructor() {
    /** The four lines shown on the sign. By default, the first line is empty for input and the rest is a hint. */
    public var lines: List<String> = listOf("", "", "^^^^^^^^^^^^^^^", "Enter query")

    /** The indices of [lines] the player types into, within `0..3`. Defaults to the first two lines. */
    public var inputLines: IntRange = 0..1

    private var onInput: ((Player, String) -> Unit)? = null

    /**
     * Sets [handler] to run when the player closes the sign editor, including with Escape. It receives the player
     * and the text of the input lines joined with spaces and trimmed.
     */
    public fun onInput(handler: (player: Player, input: String) -> Unit) {
        onInput = handler
    }

    internal fun build(): SignInput {
        val handler = checkNotNull(onInput) { "Sign input has no onInput handler" }
        return SignInput(lines.toList(), inputLines, handler)
    }
}

public class SignInput internal constructor(
    public val lines: List<String>,
    public val inputLines: IntRange,
    private val onInput: (Player, String) -> Unit,
) {
    private val signData: CompoundBinaryTag = CompoundBinaryTag.empty().put(
        "front_text",
        CompoundBinaryTag.empty().put("messages", ListBinaryTag.from(lines.map(StringBinaryTag::stringBinaryTag))),
    )

    init {
        require(lines.size == 4) { "Sign input needs exactly 4 lines, got ${lines.size}" }
        require(!inputLines.isEmpty() && inputLines.first >= 0 && inputLines.last <= 3) {
            "Input lines must be a non-empty range within 0..3, got $inputLines"
        }
    }

    /**
     * Shows a fake sign 5 blocks above [player] and opens its editor. When the player closes the editor, the
     * real block comes back and the `onInput` handler runs. If [player] already has a sign input open, it is
     * replaced and its handler never runs. Throws [IllegalStateException] if [player] is in no instance.
     */
    public fun open(player: Player) {
        val instance = checkNotNull(player.instance) { "Player ${player.username} is in no instance" }

        val previous = player.getTag(PENDING_TAG)
        if (previous != null) {
            player.eventNode().removeListener(previous.listener)
            restore(player, instance, previous.position)
        }

        val position = player.position.asBlockVec().add(0, 5, 0)

        // The client sends one edit for the replaced editor. If it was on this block, it must not count as input.
        var ignoreNext = previous?.position == position
        val listener = EventListener.builder(PlayerEditSignEvent::class.java)
            .filter { it.blockPosition == position }
            .filter { !ignoreNext.also { ignoreNext = false } }
            .expireCount(1)
            .handler { event ->
                player.removeTag(PENDING_TAG)
                restore(player, event.instance, position)

                onInput(player, event.lines.slice(inputLines).joinToString(" ").trim())
            }
            .build()

        player.eventNode().addListener(listener)
        player.setTag(PENDING_TAG, Pending(position, listener))

        player.sendPackets(
            BlockChangePacket(position, Block.OAK_SIGN),
            BlockEntityDataPacket(position, BlockEntityType.SIGN, signData),
            OpenSignEditorPacket(position, true),
        )
    }
}

private class Pending(val position: BlockVec, val listener: EventListener<PlayerEditSignEvent>)

private val PENDING_TAG: Tag<Pending> = Tag.Transient("lodestone:sign-input")

private fun restore(player: Player, instance: Instance, position: BlockVec) {
    val block = instance.getBlock(position)
    player.sendPacket(BlockChangePacket(position, block))
    val type = block.blockEntityType() ?: return
    player.sendPacket(BlockEntityDataPacket(position, type, BlockUtils.extractClientNbt(block)))
}
