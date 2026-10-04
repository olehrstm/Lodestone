package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.render.MenuElement
import de.ole101.lodestone.menu.render.Region
import de.ole101.lodestone.menu.render.RenderScope
import de.ole101.lodestone.menu.slot.ClickContext
import de.ole101.lodestone.menu.slot.SlotPolicy
import de.ole101.lodestone.reactive.*
import de.ole101.lodestone.text.glyph.TextAlign
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * The receiver of a menu setup and of [show], [key] and [each] blocks. Everything created in a block, such as
 * elements, effects, hooks and coroutines, is removed together with that block.
 *
 * All callbacks run on the tick thread. Exceptions thrown by effects, elements and hooks go to the exception manager.
 */
@MenuDsl
public class MenuScope internal constructor(
    public val session: MenuSession,
    private val reactive: ReactiveScope,
    private val region: Region,
    private val coroutineScope: CoroutineScope,
) {
    /** The players viewing this menu. Reading it in an effect or element tracks it. */
    public val viewers: Set<Player> get() = session.viewers

    public fun <T> state(initial: T): State<T> = reactive.state(initial)

    public fun <T> derived(compute: () -> T): Derived<T> = reactive.derived(compute)

    /**
     * Runs [block] after the menu rendered, and again at tick end after the states it read changed.
     * Its [EffectScope.onCleanup] callbacks run before each re-run and when this block is removed.
     */
    public fun effect(block: EffectScope.() -> Unit) {
        reactive.effect(block)
    }

    /** Adds an element that draws with [render]. It renders again whenever a state it read changes. */
    public fun element(render: RenderScope.() -> Unit) {
        val entry = Region.ElementEntry()
        region.entries += entry

        reactive.preEffect {
            val scope = RenderScope(session, coroutineScope, this)
            scope.render()
            entry.slots = scope.slots
            entry.layers = scope.layers
            session.markDirty()

            onCleanup {
                entry.slots = emptyMap()
                entry.layers = emptyList()
                session.markDirty()
            }
        }
    }

    public fun element(element: MenuElement) {
        this.element { with(element) { render() } }
    }

    /** Shows the elements of [block] while [condition] returns true. The block is set up again each time it shows. */
    public fun show(condition: () -> Boolean, block: MenuScope.() -> Unit) {
        key(condition) { shown -> if (shown) block() }
    }

    /**
     * Sets up [block] with the value [value] returns, and again from scratch whenever that value changes.
     * Everything the previous block created is removed first.
     */
    public fun <K> key(value: () -> K, block: MenuScope.(K) -> Unit) {
        val entry = Region.BlockEntry()
        region.entries += entry
        var mounted: Mounted? = null
        // Derived skips equal values, so the block is only set up again when the value really changes
        val key = reactive.derived(value)

        reactive.preEffect {
            val current = key.value
            untrack {
                mounted?.scope?.dispose()
                mounted = null
                entry.regions = emptyList()
                session.markDirty()

                val next = mount { block(current) }
                mounted = next
                entry.regions = listOf(next.region)
            }
        }
    }

    /**
     * Sets up [block] once for each item of the list [items] returns. When the list changes, blocks of items that
     * are still in it (compared with `==`) are kept, blocks of removed items are removed, and new items get a new block.
     */
    public fun <T> each(items: () -> List<T>, block: MenuScope.(T) -> Unit) {
        val entry = Region.BlockEntry()
        region.entries += entry
        var mounted: List<Pair<T, Mounted>> = emptyList()

        reactive.preEffect {
            val current = items()
            untrack {
                val unused = mounted.toMutableList()
                mounted = current.map { item ->
                    val index = unused.indexOfFirst { it.first == item }
                    if (index >= 0) unused.removeAt(index) else item to mount { block(item) }
                }
                unused.forEach { it.second.scope.dispose() }
                entry.regions = mounted.map { it.second.region }
                session.markDirty()
            }
        }
    }

    /**
     * Launches [block] in this block's coroutine scope on the Minestom tick dispatcher. It is cancelled when this
     * block is removed or the session is disposed. [context] and [start] work like in [kotlinx.coroutines.launch].
     */
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = coroutineScope.launch(context, start, block)

    /**
     * Draws [text] as the title, aligned with [align] inside the menu width. Left-aligned titles start where the
     * chest's own title would. The title draws on top of all other
     * text and glyphs.
     */
    public fun title(align: TextAlign = TextAlign.LEFT, text: () -> Component) {
        element { title(text(), align) }
    }

    public fun onOpen(hook: (viewer: Player) -> Unit) {
        addHook(session.openHooks, hook)
    }

    /**
     * Runs [hook] after a player left this menu. Players who open another inventory or leave the server are noticed
     * at the start of the next tick.
     */
    public fun onClose(hook: (viewer: Player, reason: CloseReason) -> Unit) {
        addHook(session.closeHooks, hook)
    }

    /** Runs [hook] for every click on a menu slot, before the slot's own handler. It may change [ClickContext.isCancelled]. */
    public fun onClick(hook: (ClickContext) -> Unit) {
        addHook(session.clickHooks, hook)
    }

    /**
     * Ignores clicks on menu slots that a player makes within [cooldown] after their last accepted click.
     * They are cancelled and reach no handler. Applies to the whole session, also when called in a block, and replaces an
     * earlier cooldown. There is none by default. Throws [IllegalArgumentException] if [cooldown] is negative.
     */
    public fun clickCooldown(cooldown: Duration) {
        require(!cooldown.isNegative()) { "Click cooldown must not be negative, but was $cooldown" }
        session.clickCooldown = cooldown
    }

    /**
     * Runs [hook] for every click of a viewer in their own inventory below the menu. [ClickContext.slot] is the slot
     * in the player's inventory. Clicks move items as usual unless the hook sets [ClickContext.isCancelled].
     * Shift clicks move items only into [SlotPolicy.PLACE] and [SlotPolicy.FREE] slots that accept them.
     */
    public fun onPlayerInventoryClick(hook: (ClickContext) -> Unit) {
        addHook(session.playerInventoryClickHooks, hook)
    }

    /**
     * Runs [hook] when a viewer clicks outside the GUI. The client does not tell where, so any click outside counts.
     * [ClickContext.slot] is -999. A click with an item on the cursor drops it as usual unless the hook sets
     * [ClickContext.isCancelled].
     */
    public fun onOutsideClick(hook: (ClickContext) -> Unit) {
        addHook(session.outsideClickHooks, hook)
    }

    /** Runs [hook] at the start of every tick while the session exists. */
    public fun onTick(hook: () -> Unit) {
        addHook(session.tickHooks, hook)
    }

    private fun <H> addHook(hooks: MutableList<H>, hook: H) {
        hooks += hook
        reactive.onDispose { hooks -= hook }
    }

    private fun mount(block: MenuScope.() -> Unit): Mounted {
        val child = reactive.child()
        val childRegion = Region()
        val childCoroutines = CoroutineScope(coroutineScope.coroutineContext + SupervisorJob(coroutineScope.coroutineContext.job))
        child.onDispose { childCoroutines.cancel() }

        MenuScope(session, child, childRegion, childCoroutines).block()
        return Mounted(child, childRegion)
    }

    private class Mounted(val scope: ReactiveScope, val region: Region)
}
