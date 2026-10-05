package de.ole101.lodestone.menu

import de.ole101.lodestone.coroutine.tickScope
import de.ole101.lodestone.event.eventNode
import de.ole101.lodestone.event.listen
import de.ole101.lodestone.exceptionManager
import de.ole101.lodestone.globalEventHandler
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.layout.checkSlot
import de.ole101.lodestone.menu.layout.inventoryType
import de.ole101.lodestone.menu.layout.slotIndex
import de.ole101.lodestone.menu.render.Frame
import de.ole101.lodestone.menu.render.Region
import de.ole101.lodestone.menu.slot.*
import de.ole101.lodestone.reactive.ReactiveScope
import de.ole101.lodestone.reactive.state
import de.ole101.lodestone.reactive.untrack
import de.ole101.lodestone.schedulerManager
import kotlinx.coroutines.cancel
import net.kyori.adventure.text.Component
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.timer.TaskSchedule
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

/**
 * A live menu. It is one inventory with its state that all its viewers share. Create it with [MenuBlueprint.create].
 *
 * State changes re-render the menu once per tick, at tick end. Use a session only on the tick thread.
 */
public class MenuSession internal constructor(
    public val type: MenuType,
    private val disposePolicy: DisposePolicy,
    setup: MenuScope.() -> Unit,
) {
    internal val inventory: Inventory = Inventory(type.inventoryType, Component.empty())
    internal val openHooks = CopyOnWriteArrayList<(Player) -> Unit>()
    internal val closeHooks = CopyOnWriteArrayList<(Player, CloseReason) -> Unit>()
    internal val clickHooks = CopyOnWriteArrayList<(ClickContext) -> Unit>()
    internal val playerInventoryClickHooks = CopyOnWriteArrayList<(ClickContext) -> Unit>()
    internal val outsideClickHooks = CopyOnWriteArrayList<(ClickContext) -> Unit>()
    internal val tickHooks = CopyOnWriteArrayList<() -> Unit>()
    internal var clickCooldown: Duration = Duration.ZERO

    private val contentsVersion = state(0)
    private val coroutineScope = tickScope()
    private val reactive = ReactiveScope()
    private val root = Region()
    private val node = eventNode("lodestone:menu")
    private val task = schedulerManager.buildTask(::tick).repeat(TaskSchedule.nextTick()).schedule()
    private var frame = Frame.EMPTY
    private var dirty = false
    private var applying = false

    // Who last put items into each input slot, to give them back. A stack filled by several players goes to the last
    // one. Viewers may click on different tick threads.
    private val owners = ConcurrentHashMap<Int, Player>()

    // The session each player goes back to from here, and the players who may come back here from a later menu
    private val backs = ConcurrentHashMap<Player, MenuSession>()
    private val away: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    // Players answering a prompt, with a token per prompt so an older prompt cannot end a newer one
    private val prompts = ConcurrentHashMap<Player, Any>()

    private val lastClicks = ConcurrentHashMap<UUID, Long>()

    /** Whether [dispose] ran. A disposed session cannot be opened. */
    public var isDisposed: Boolean = false
        private set

    /** The players viewing this session. Reading it in an effect or element tracks it. */
    public var viewers: Set<Player> by state(emptySet())
        private set

    private val currentViewers: Set<Player> get() = untrack { viewers }

    init {
        listen<InventoryPreClickEvent>(node, ::handleClick)
        listen<InventoryCloseEvent>(node) { event ->
            if (event.inventory !== inventory) return@listen
            val reason = when {
                event.player in away -> CloseReason.NAVIGATED
                event.isFromClient -> CloseReason.PLAYER
                else -> CloseReason.SERVER
            }
            removeViewer(event.player, reason)
        }
        listen<InventoryItemChangeEvent>(node, ::handleChange)
        reactive.afterFlush(::commit)

        try {
            MenuScope(this, reactive, root, coroutineScope).setup()
            reactive.flush()
        } catch (e: Throwable) {
            dispose()
            throw e
        }
    }

    /**
     * Opens this session for [player] and runs the `onOpen` hooks. Does nothing if [player] already views it or an
     * event cancels the opening. Players opened into one session share its state.
     *
     * @throws IllegalStateException if this session is disposed.
     */
    public fun open(player: Player) {
        check(!isDisposed) { "$this is disposed" }
        openViewer(player)
    }

    /**
     * Opens this session for [player] with [back] as the way back, for example from a click in [back].
     * [back] stays alive while the player can return to it, even with [DisposePolicy.WHEN_EMPTY], and keeps its
     * state. [goBack] returns there. When the player leaves this session any other way, the whole way back is released
     * and disposed like a closed session. Does nothing if an event cancels the opening.
     *
     * @throws IllegalStateException if this session or [back] is disposed.
     * @throws IllegalArgumentException if [back] is this session.
     */
    public fun open(player: Player, back: MenuSession) {
        check(!isDisposed) { "$this is disposed" }
        check(!back.isDisposed) { "$back is disposed" }
        require(back !== this) { "A session cannot be its own way back" }

        back.away += player
        if (!openViewer(player)) {
            back.release(player)
            return
        }
        backs.put(player, back)?.takeIf { it !== back }?.release(player)
        back.removeViewer(player, CloseReason.NAVIGATED)
    }

    /**
     * Returns [player] to the session they came from with the `open` overload with a way back, which shows its state
     * as they left it. Returns false and does nothing if there is no way back or it was disposed in the meantime.
     */
    public fun goBack(player: Player): Boolean {
        val back = backs[player] ?: return false
        if (back.isDisposed) {
            backs -= player
            return false
        }

        back.away -= player
        if (!back.openViewer(player)) {
            back.away += player
            return false
        }
        backs -= player
        // Going back leaves this session for good, so the player's items come back
        returnItemsOf(player)
        removeViewer(player, CloseReason.NAVIGATED)
        return true
    }

    public fun canGoBack(player: Player): Boolean = backs[player]?.isDisposed == false

    private fun openViewer(player: Player): Boolean {
        if (player in currentViewers || !player.openInventory(inventory)) return false

        viewers = currentViewers + player
        openHooks.forEach { hook -> safely { hook(player) } }
        return true
    }

    // The player will not come back here, so neither to the sessions before this one
    private fun release(player: Player) {
        away -= player
        backs.remove(player)?.release(player)
        disposeIfUnused()
    }

    /**
     * Keeps this session for [player] while they answer a prompt outside it, and closes it for them if they view it.
     * Returns the token that [endPrompt] needs.
     */
    internal fun beginPrompt(player: Player): Any {
        check(!isDisposed) { "$this is disposed" }
        val token = Any()
        prompts[player] = token
        away += player
        if (player.openInventory === inventory) player.closeInventory()
        return token
    }

    /**
     * Ends the prompt with [token] and opens this session for [player] again, unless a newer prompt started, the
     * player went offline, or something else opened an inventory for them in the meantime.
     */
    internal fun endPrompt(player: Player, token: Any) {
        if (!prompts.remove(player, token)) return
        away -= player
        if (!isDisposed && player.isOnline && player.openInventory == null) openViewer(player)
        disposeIfUnused()
    }

    private fun disposeIfUnused() {
        if (disposePolicy == DisposePolicy.WHEN_EMPTY && currentViewers.isEmpty() && away.isEmpty()) dispose()
    }

    /**
     * Returns the item in [slot], also one a player put there. Reading it in an effect or element tracks changes by
     * players and by [set]. Items the menu renders change together with the states they come from.
     * Throws [IllegalArgumentException] if [slot] is outside the menu.
     */
    public operator fun get(slot: Int): ItemStack {
        type.checkSlot(slot)
        contentsVersion.value
        return inventory.getItemStack(slot)
    }

    public operator fun get(column: Int, row: Int): ItemStack = get(type.slotIndex(column, row))

    /**
     * Puts [item] into [slot], for example to take the items a player placed into an input slot. It counts as a
     * change for `onChange` handlers. The next render only replaces it when the slot's `item { }` result changes.
     * Throws [IllegalArgumentException] if [slot] is outside the menu.
     */
    public operator fun set(slot: Int, item: ItemStack) {
        type.checkSlot(slot)
        // The menu owns what it puts there, so it is never given to a player
        owners -= slot
        inventory.setItemStack(slot, item)
    }

    public operator fun set(column: Int, row: Int, item: ItemStack) {
        set(type.slotIndex(column, row), item)
    }

    /** Closes this session for all viewers. With [DisposePolicy.WHEN_EMPTY] this disposes it. */
    public fun close() {
        currentViewers.forEach { if (it.openInventory === inventory) it.closeInventory() }
    }

    /**
     * Closes this session for all viewers and frees it. It cancels its coroutines, runs every `onCleanup`, gives items
     * players put into input slots back to them and releases the way back of every player.
     * Repeated calls do nothing.
     */
    public fun dispose() {
        if (isDisposed) return
        isDisposed = true

        close()
        for (slot in owners.keys.toList()) returnItem(slot)
        for (player in backs.keys.toList()) backs.remove(player)?.release(player)
        task.cancel()
        coroutineScope.cancel()
        reactive.dispose()
        globalEventHandler.removeChild(node)
    }

    internal fun markDirty() {
        dirty = true
    }

    private fun commit() {
        if (!dirty || isDisposed) return
        dirty = false

        val previous = frame
        frame = root.frame()
        withoutChangeEvents {
            // Items in a slot that stopped being an input slot go back to whoever put them there
            for (slot in 0 until inventory.size) {
                if (previous.slots[slot].isInput() && !frame.slots[slot].isInput()) returnItem(slot)
                val item = frame.slots[slot]?.item ?: ItemStack.AIR
                if (item != (previous.slots[slot]?.item ?: ItemStack.AIR)) inventory.setItemStack(slot, item)
            }
        }
        // A title that cannot be built must not break the slots
        if (frame.layers != previous.layers) safely { inventory.title = frame.title(type) }
    }

    private fun tick() {
        for (viewer in currentViewers) {
            if (viewer.openInventory !== inventory) {
                removeViewer(viewer, if (viewer.isOnline) CloseReason.REPLACED else CloseReason.DISCONNECTED)
            }
        }
        // A player who leaves the server never answers their prompt
        for ((player, token) in prompts) {
            if (!player.isOnline) endPrompt(player, token)
        }
        tickHooks.forEach { safely(it) }
    }

    private fun removeViewer(player: Player, reason: CloseReason) {
        if (player !in currentViewers) return

        viewers = currentViewers - player
        lastClicks -= player.uuid
        if (reason != CloseReason.NAVIGATED) {
            returnItemsOf(player)
            backs.remove(player)?.release(player)
        }
        closeHooks.forEach { hook -> safely { hook(player, reason) } }
        disposeIfUnused()
    }

    private fun handleClick(event: InventoryPreClickEvent) {
        val player = event.player
        val click = event.click
        val cursor = player.inventory.cursorItem

        if (event.inventory !== inventory) {
            if (player.openInventory === inventory) handlePlayerInventoryClick(event)
            return
        }

        if (isCoolingDown(player)) {
            event.isCancelled = true
            return
        }

        when (click) {
            is Click.Drag -> {
                val menuSlots = click.slots().filter { it < inventory.size }
                event.isCancelled = click is Click.MiddleDrag || !menuSlots.all { canPlace(it, cursor) }
                if (!event.isCancelled) menuSlots.forEach { owners[it] = player }
                return
            }

            is Click.Double -> {
                event.isCancelled = !canCollect(cursor)
                return
            }

            else -> {}
        }

        val slot = click.slot()
        if (slot !in 0 until inventory.size) {
            event.isCancelled = true
            return
        }

        val view = frame.slots[slot]
        val swapped = when (click) {
            is Click.HotbarSwap -> player.inventory.getItemStack(click.hotbarSlot())
            is Click.OffhandSwap -> player.getEquipment(EquipmentSlot.OFF_HAND)
            else -> ItemStack.AIR
        }
        val allowed = view != null && view.policy.allows(click, inventory.getItemStack(slot), cursor, swapped, view.accepts)
        val context = ClickContext(player, slot, click, isCancelled = !allowed)
        clickHooks.forEach { it(context) }
        view?.onClick?.invoke(context)
        event.isCancelled = context.isCancelled
        if (!context.isCancelled && view.isInput()) owners[slot] = player
    }

    private fun handlePlayerInventoryClick(event: InventoryPreClickEvent) {
        val player = event.player
        val click = event.click

        // The client reports clicks outside the GUI as dropping the cursor, also when the cursor is empty
        if (click is Click.DropCursor) {
            val context = ClickContext(player, click.slot(), click, isCancelled = false)
            outsideClickHooks.forEach { it(context) }
            event.isCancelled = context.isCancelled
            return
        }
        // Double clicks also collect from the menu, so they must not take items the menu owns
        val context = ClickContext(player, click.slot(), click, isCancelled = click is Click.Double && !canCollect(player.inventory.cursorItem))
        playerInventoryClickHooks.forEach { it(context) }

        if (!context.isCancelled && (click is Click.LeftShift || click is Click.RightShift)) {
            // Minestom would fill any menu slot, so only input slots are filled here
            moveIntoInputSlots(player, click.slot())
            event.isCancelled = true
            return
        }
        event.isCancelled = context.isCancelled
    }

    private fun canPlace(slot: Int, item: ItemStack): Boolean {
        val view = frame.slots[slot] ?: return false
        val current = inventory.getItemStack(slot)
        return view.policy.canPlace && view.accepts(item) &&
                (current.isAir || current.isSimilar(item))
    }

    private fun canCollect(cursor: ItemStack): Boolean = (0 until inventory.size).none { slot ->
        frame.slots[slot]?.policy?.canTake != true && inventory.getItemStack(slot).isSimilar(cursor)
    }

    // Stacks onto matching input slots first, then fills empty ones, like a shift click into a chest
    private fun moveIntoInputSlots(player: Player, playerSlot: Int) {
        var remaining = player.inventory.getItemStack(playerSlot)
        if (remaining.isAir) return

        val targets = (0 until inventory.size).filter { canPlace(it, remaining) }
        for (fillEmpty in listOf(false, true)) {
            for (slot in targets) {
                if (remaining.isAir) break
                val current = inventory.getItemStack(slot)
                if (current.isAir != fillEmpty) continue

                val stacked = if (fillEmpty) 0 else current.amount()
                val moved = minOf(remaining.amount(), remaining.maxStackSize() - stacked)
                if (moved <= 0) continue
                inventory.setItemStack(slot, remaining.withAmount(stacked + moved))
                owners[slot] = player
                remaining = if (remaining.amount() == moved) ItemStack.AIR else remaining.withAmount(remaining.amount() - moved)
            }
        }
        player.inventory.setItemStack(playerSlot, remaining)
    }

    // Records the click unless it came too soon after the last recorded one
    private fun isCoolingDown(player: Player): Boolean {
        if (!clickCooldown.isPositive()) return false

        val now = System.nanoTime()
        val last = lastClicks[player.uuid]
        if (last != null && now - last < clickCooldown.inWholeNanoseconds) return true
        lastClicks[player.uuid] = now
        return false
    }

    private fun handleChange(event: InventoryItemChangeEvent) {
        if (event.inventory !== inventory || applying) return
        if (event.newItem.isAir) owners -= event.slot
        contentsVersion.value++
        frame.slots[event.slot]?.onChange?.invoke(event.previousItem, event.newItem)
    }

    private fun returnItemsOf(player: Player) {
        owners.filterValues { it == player }.keys.forEach(::returnItem)
    }

    // Gives the item in slot to the player who put it there. Offline players cannot get it back, so it stays.
    private fun returnItem(slot: Int) {
        val owner = owners[slot] ?: return
        if (!owner.isOnline) return
        owners -= slot

        val item = inventory.getItemStack(slot)
        if (item.isAir) return
        withoutChangeEvents { inventory.setItemStack(slot, ItemStack.AIR) }
        contentsVersion.value++
        if (!owner.inventory.addItemStack(item)) owner.dropItem(item)
    }

    // Changes made by the menu itself are not reported to onChange handlers
    private inline fun withoutChangeEvents(block: () -> Unit) {
        val wasApplying = applying
        applying = true
        try {
            block()
        } finally {
            applying = wasApplying
        }
    }

    private fun SlotView?.isInput(): Boolean = this?.policy?.canPlace == true

    override fun toString(): String = "MenuSession(type=$type, viewers=${currentViewers.size}, disposed=$isDisposed)"
}

internal inline fun safely(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        exceptionManager.handleException(e)
    }
}
