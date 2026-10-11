package de.ole101.lodestone.menu

import de.ole101.lodestone.coroutine.tickScope
import de.ole101.lodestone.event.eventNode
import de.ole101.lodestone.event.listen
import de.ole101.lodestone.globalEventHandler
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.render.Frame
import de.ole101.lodestone.menu.render.Region
import de.ole101.lodestone.menu.session.AnvilField
import de.ole101.lodestone.menu.session.ClickHandler
import de.ole101.lodestone.menu.session.InputSlots
import de.ole101.lodestone.menu.session.MenuHooks
import de.ole101.lodestone.menu.session.Navigation
import de.ole101.lodestone.menu.session.PlayerSlotOverlay
import de.ole101.lodestone.menu.slot.isInput
import de.ole101.lodestone.reactive.ReactiveScope
import de.ole101.lodestone.reactive.node.safely
import de.ole101.lodestone.reactive.node.state
import de.ole101.lodestone.reactive.node.untracked
import de.ole101.lodestone.schedulerManager
import kotlinx.coroutines.cancel
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.player.PlayerAnvilInputEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.timer.TaskSchedule

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
    internal val inventory = Inventory(type.inventoryType, Component.empty())
    internal val hooks = MenuHooks()

    private val contentsVersion = state(0)
    private val coroutineScope = tickScope()
    private val reactive = ReactiveScope()
    private val root = Region()
    private val node = eventNode("lodestone:menu")
    private val task = schedulerManager.buildTask(::tick).repeat(TaskSchedule.nextTick()).schedule()
    private var dirty = false
    private var applying = false

    internal var frame = Frame.EMPTY
        private set

    internal val anvil = AnvilField(this)
    internal val clicks = ClickHandler(this)
    internal val inputSlots = InputSlots(this)
    internal val playerOverlay = PlayerSlotOverlay(this)
    internal val navigation = Navigation(this)

    /**
     * Whether [dispose] ran. A disposed session cannot be opened.
     */
    public var isDisposed: Boolean = false
        private set

    /**
     * The players viewing this session. Reading it in an effect or element tracks it.
     */
    public var viewers: Set<Player> by state(emptySet())
        private set

    internal val currentViewers: Set<Player>
        get() = untracked { viewers }

    /**
     * The current anvil rename text, initially empty. Reading it in an element tracks changes.
     * Typed text keeps only printable ASCII characters and is trimmed. It is empty for chest menus.
     * Anvil text is shared by the viewers of this session.
     */
    public val anvilInput: String
        get() = anvil.text

    init {
        listen<InventoryPreClickEvent>(node, clicks::handle)
        listen<InventoryCloseEvent>(node) { event ->
            if (event.inventory !== inventory) return@listen
            removeViewer(event.player, closeReason(event))
        }
        listen<InventoryItemChangeEvent>(node, ::handleChange)
        listen<PlayerDisconnectEvent>(node) { event -> inputSlots.returnItemsOf(event.player) }
        listen<PlayerAnvilInputEvent>(node, anvil::handleInput)
        reactive.afterFlush(::commit)

        try {
            MenuScope(this, reactive, root, coroutineScope).setup()
            reactive.flush()
        } catch (exception: Throwable) {
            dispose()
            throw exception
        }
    }

    /**
     * Opens this session for [player] and runs the `onOpen` hooks. Does nothing if [player] already views it or an
     * event cancels the opening. Players opened into one session share its state.
     *
     * @throws IllegalStateException if this session is disposed.
     */
    public fun open(player: Player) {
        checkNotDisposed()
        openViewer(player)
    }

    /**
     * Opens this session for [player] with [back] as the way back, for example from a click in [back]. [back] keeps its
     * state and stays alive while the player can return to it, even with [DisposePolicy.WHEN_EMPTY]. [goBack] returns
     * there. When the player leaves this session any other way, [back] is released like a closed session. Does nothing
     * if an event cancels the opening.
     *
     * @throws IllegalStateException if this session or [back] is disposed.
     * @throws IllegalArgumentException if [back] is this session.
     */
    public fun open(
        player: Player,
        back: MenuSession,
    ) {
        checkNotDisposed()
        check(!back.isDisposed) { "$back is disposed" }
        require(back !== this) { "A session cannot be its own way back" }

        navigation.open(player, back)
    }

    /**
     * Returns [player] to the session they came from, showing its state as they left it.
     * Returns false and does nothing if there is no way back or it was disposed in the meantime.
     */
    public fun goBack(player: Player): Boolean = navigation.goBack(player)

    public fun canGoBack(player: Player): Boolean = navigation.canGoBack(player)

    internal fun openViewer(player: Player): Boolean {
        if (player in currentViewers) return false
        anvil.seedIfConfigured()
        if (!player.openInventory(inventory)) return false

        viewers = currentViewers + player
        if (frame.playerSlots.isNotEmpty()) playerOverlay.show(player)
        hooks.open.forEach { hook -> safely { hook(player) } }
        return true
    }

    internal fun disposeIfUnused() {
        if (disposePolicy == DisposePolicy.WHEN_EMPTY && currentViewers.isEmpty() && navigation.hasNoAway) dispose()
    }

    /**
     * Returns the item in [slot], also one a player put there. Reading it in an effect or element tracks changes by
     * players and by [set]. Items the menu renders change together with the states they come from.
     *
     * @throws IllegalArgumentException if [slot] is outside the menu.
     */
    public operator fun get(slot: Int): ItemStack {
        type.checkSlot(slot)
        contentsVersion.value
        return inventory.getItemStack(slot)
    }

    public operator fun get(
        column: Int,
        row: Int,
    ): ItemStack = get(type.slotIndex(column, row))

    /**
     * Puts [item] into [slot], for example to take the items a player placed into an input slot. It counts as a
     * change for `onChange` handlers. The next render only replaces it when the slot's `item { }` result changes.
     *
     * @throws IllegalArgumentException if [slot] is outside the menu.
     */
    public operator fun set(
        slot: Int,
        item: ItemStack,
    ) {
        type.checkSlot(slot)
        // The menu owns what it puts there, so it is never given to a player
        inputSlots.forget(slot)
        inventory.setItemStack(slot, item)
    }

    public operator fun set(
        column: Int,
        row: Int,
        item: ItemStack,
    ) {
        set(type.slotIndex(column, row), item)
    }

    /**
     * Closes this session for all viewers. With [DisposePolicy.WHEN_EMPTY] this disposes it once no player can come
     * back to it.
     */
    public fun close() {
        currentViewers
            .filter { it.openInventory === inventory }
            .forEach(Player::closeInventory)
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
        inputSlots.returnAll()
        navigation.releaseAllBacks()
        task.cancel()
        coroutineScope.cancel()
        reactive.dispose()
        globalEventHandler.removeChild(node)
    }

    internal fun markDirty() {
        dirty = true
    }

    internal fun markContentsChanged() {
        contentsVersion.value++
    }

    internal fun removeViewer(
        player: Player,
        reason: CloseReason,
    ) {
        if (player !in currentViewers) return

        viewers = currentViewers - player
        clicks.cooldown.forget(player)
        playerOverlay.restore(player)
        if (reason != CloseReason.NAVIGATED) {
            inputSlots.returnItemsOf(player)
            navigation.releaseBack(player)
        }
        hooks.close.forEach { hook -> safely { hook(player, reason) } }
        disposeIfUnused()
    }

    // Changes made by the menu itself are not reported to onChange handlers
    internal inline fun withoutChangeEvents(block: () -> Unit) {
        val wasApplying = applying
        applying = true
        try {
            block()
        } finally {
            applying = wasApplying
        }
    }

    internal fun checkNotDisposed() = check(!isDisposed) { "$this is disposed" }

    private fun closeReason(event: InventoryCloseEvent): CloseReason = when {
        navigation.isAway(event.player) -> CloseReason.NAVIGATED
        event.isFromClient -> CloseReason.PLAYER
        else -> CloseReason.SERVER
    }

    private fun tick() {
        for (viewer in currentViewers) {
            if (viewer.openInventory === inventory) continue
            removeViewer(viewer, if (viewer.isOnline) CloseReason.REPLACED else CloseReason.DISCONNECTED)
        }
        navigation.endOfflinePrompts()
        hooks.tick.forEach { safely(it) }
    }

    private fun commit() {
        if (!dirty || isDisposed) return
        dirty = false

        val previous = frame
        frame = root.frame()
        applySlots(previous)
        if (frame.playerItems != previous.playerItems) currentViewers.forEach(playerOverlay::show)
        if (frame.itemAt(0) != previous.itemAt(0)) anvil.seedIfConfigured()
        if (frame.layers != previous.layers) applyTitle()
    }

    private fun applySlots(previous: Frame) = withoutChangeEvents {
        for (slot in 0 until inventory.size) {
            // Items in a slot that stopped being an input slot go back to whoever put them there
            if (previous.slots[slot].isInput && !frame.slots[slot].isInput) inputSlots.returnItem(slot)

            val item = frame.itemAt(slot)
            if (item != previous.itemAt(slot)) inventory.setItemStack(slot, item)
        }
    }

    private fun applyTitle() {
        safely {
            // Changing the title reopens the window
            anvil.seedIfConfigured()
            inventory.title = frame.title(type)
        }
    }

    private fun handleChange(event: InventoryItemChangeEvent) {
        when {
            event.inventory is PlayerInventory -> playerOverlay.showAgainAfterChange(event.inventory)
            event.inventory === inventory && !applying -> handleMenuChange(event)
        }
    }

    private fun handleMenuChange(event: InventoryItemChangeEvent) {
        if (event.newItem.isAir) inputSlots.forget(event.slot)
        markContentsChanged()
        frame.slots[event.slot]?.onChange?.invoke(event.previousItem, event.newItem)
    }

    override fun toString(): String = "MenuSession(type=$type, viewers=${currentViewers.size}, disposed=$isDisposed)"
}
