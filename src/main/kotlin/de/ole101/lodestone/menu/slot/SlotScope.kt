package de.ole101.lodestone.menu.slot

import de.ole101.lodestone.menu.MenuDsl
import de.ole101.lodestone.menu.MenuSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minestom.server.item.ItemStack
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Configures one menu slot. The block that configures it runs again each time the element around it re-renders,
 * so reads of states in it are tracked.
 */
@MenuDsl
public class SlotScope internal constructor(
    /**
     * The session this slot belongs to. The DSL marker hides the menu scope inside slot blocks, so use it for slot
     * contents or viewers there.
     */
    public val session: MenuSession,
    private val coroutineScope: CoroutineScope,
) {
    /** What players may do with the item in this slot. Defaults to [SlotPolicy.LOCKED]. */
    public var policy: SlotPolicy = SlotPolicy.LOCKED

    private var item: ItemStack = ItemStack.AIR
    private var onClick: ((ClickContext) -> Unit)? = null
    private var accepts: (ItemStack) -> Boolean = { true }
    private var onChange: ((ItemStack, ItemStack) -> Unit)? = null

    /**
     * Shows the item that [block] returns. The slot is only updated when the result changes, so items players put
     * into an input slot stay until then.
     */
    public fun item(block: () -> ItemStack) {
        item = block()
    }

    /**
     * Runs [handler] on the clicking player's tick thread when a player clicks this slot, also when the click is
     * cancelled by [policy]. Exceptions go to the exception manager.
     */
    public fun onClick(handler: (ClickContext) -> Unit) {
        onClick = handler
    }

    /** Lets players only place items for which [predicate] returns true. Only matters with [SlotPolicy.PLACE] or [SlotPolicy.FREE]. */
    public fun accepts(predicate: (ItemStack) -> Boolean) {
        accepts = predicate
    }

    /**
     * Runs [handler] with the old and new item after a player changed the item in this slot. Changes made by the
     * menu itself do not count. Exceptions go to the exception manager.
     */
    public fun onChange(handler: (old: ItemStack, new: ItemStack) -> Unit) {
        onChange = handler
    }

    /**
     * Launches [block] in the coroutine scope of the menu block that declared this slot, which is cancelled when that
     * block is removed or the session is disposed. Works like [kotlinx.coroutines.launch].
     */
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = coroutineScope.launch(context, start, block)

    internal fun build(): SlotView = SlotView(item, policy, onClick, accepts, onChange)
}

internal class SlotView(
    val item: ItemStack,
    val policy: SlotPolicy,
    val onClick: ((ClickContext) -> Unit)?,
    val accepts: (ItemStack) -> Boolean,
    val onChange: ((ItemStack, ItemStack) -> Unit)?,
)
