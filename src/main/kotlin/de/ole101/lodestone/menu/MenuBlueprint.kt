package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.layout.MenuType
import net.minestom.server.entity.Player

/**
 * Creates a reusable chest menu with [rows] rows of 9 slots. [setup] runs once per [MenuSession], with the props passed
 * to [MenuBlueprint.create]. Throws [IllegalArgumentException] if [rows] is outside `1..6`.
 *
 * @see MenuScope
 */
public fun <P> menu(rows: Int = 6, setup: MenuScope.(props: P) -> Unit): MenuBlueprint<P> = MenuBlueprint(MenuType.chest(rows), setup)

public fun menu(rows: Int = 6, setup: MenuScope.() -> Unit): MenuBlueprint<Unit> = MenuBlueprint(MenuType.chest(rows)) { setup() }

/** Creates a reusable menu of [type], running [setup] once per session with its props on the tick thread. */
public fun <P> menu(type: MenuType, setup: MenuScope.(props: P) -> Unit): MenuBlueprint<P> = MenuBlueprint(type, setup)

/** Creates a reusable menu of [type], running [setup] once per session on the tick thread. */
public fun menu(type: MenuType, setup: MenuScope.() -> Unit): MenuBlueprint<Unit> = MenuBlueprint(type) { setup() }

/**
 * An immutable description of a menu. It holds no state and no players, so one blueprint can serve any number of
 * sessions at once. Create a blueprint with [menu].
 */
public class MenuBlueprint<P> internal constructor(
    public val type: MenuType,
    private val setup: MenuScope.(P) -> Unit,
) {
    /**
     * Creates a session and runs the setup with [props]. The session renders before this returns, but nobody
     * sees it until [MenuSession.open]. [disposePolicy] decides whether the session is disposed when its last viewer
     * leaves. Exceptions thrown by the setup reach the caller. Call it on the tick thread.
     */
    public fun create(props: P, disposePolicy: DisposePolicy = DisposePolicy.WHEN_EMPTY): MenuSession =
        MenuSession(type, disposePolicy) { setup(props) }

    public fun open(player: Player, props: P): MenuSession = create(props).also { it.open(player) }

    public fun open(player: Player, props: P, back: MenuSession): MenuSession = create(props).also { it.open(player, back) }
}

public fun MenuBlueprint<Unit>.create(disposePolicy: DisposePolicy = DisposePolicy.WHEN_EMPTY): MenuSession = create(Unit, disposePolicy)

public fun MenuBlueprint<Unit>.open(player: Player): MenuSession = open(player, Unit)

public fun MenuBlueprint<Unit>.open(player: Player, back: MenuSession): MenuSession = open(player, Unit, back)

public enum class DisposePolicy {
    /** When its last viewer leaves and no player can come back to it (see [CloseReason.NAVIGATED]). */
    WHEN_EMPTY,

    /** Only by [MenuSession.dispose]. The session keeps its state while nobody views it, so it can be opened again. */
    MANUAL,
}

public enum class CloseReason {
    PLAYER,

    /** The server closed it, for example with [MenuSession.close]. */
    SERVER,

    /** The player opened another inventory. */
    REPLACED,

    /**
     * The player moved on and may return. They opened a menu with this session as its way back, went back from this
     * session, or answer a prompt such as `promptSign`. Going forward or to a prompt keeps their items in this
     * session's input slots for when they return. Going back gives them back.
     */
    NAVIGATED,

    DISCONNECTED,
}
