package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.MenuDsl
import de.ole101.lodestone.menu.MenuScope
import de.ole101.lodestone.menu.MenuSession
import de.ole101.lodestone.menu.layout.MenuStyle
import de.ole101.lodestone.menu.layout.Position
import de.ole101.lodestone.menu.layout.cell
import de.ole101.lodestone.menu.layout.playerCell
import de.ole101.lodestone.menu.render.RenderScope
import de.ole101.lodestone.menu.render.hoverSprite
import de.ole101.lodestone.menu.render.requireWidth
import de.ole101.lodestone.menu.slot.ClickContext
import de.ole101.lodestone.menu.slot.SlotScope
import de.ole101.lodestone.text.glyph.Glyph
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The sprites of a button. [normal] is the default, [pressed] shows for the moment after a click and [disabled]
 * shows while it is disabled.
 */
public class ButtonGlyphs(
    public val normal: Glyph,
    public val pressed: Glyph,
    public val disabled: Glyph = normal,
)

/**
 * How long a button shows its pressed sprite and ignores further clicks, unless a button sets its own press duration.
 */
public val DEFAULT_BUTTON_PRESS_DURATION: Duration = 300.milliseconds

/**
 * Adds a button with [glyphs] centered over [slot] and an invisible item in the slot for clicks and the tooltip.
 * The glyph is centered vertically too when it has a [Glyph.height]. A click on an enabled button shows
 * [ButtonGlyphs.pressed] for [pressDuration] and ignores further clicks until then. While the button is disabled, it shows
 * [ButtonGlyphs.disabled].
 *
 * @throws IllegalArgumentException if [slot] is outside the menu, a glyph has no width, or [pressDuration] is negative.
 */
public fun MenuScope.button(
    slot: Int,
    glyphs: ButtonGlyphs,
    pressDuration: Duration = DEFAULT_BUTTON_PRESS_DURATION,
    block: ButtonScope.() -> Unit,
) {
    val (column, row) = session.type.cellOf(slot)
    button(column..column, row, glyphs, pressDuration, block)
}

public fun MenuScope.button(
    column: Int,
    row: Int,
    glyphs: ButtonGlyphs,
    pressDuration: Duration = DEFAULT_BUTTON_PRESS_DURATION,
    block: ButtonScope.() -> Unit,
) {
    button(column..column, row, glyphs, pressDuration, block)
}

/**
 * Adds a button that spans the slots in [columns] of [row], for glyphs wider than one slot. The glyph is centered
 * over all of them, and each slot gets the invisible item, so a click on any of them presses the button.
 * See the `button` overload with a slot index for the rest.
 */
public fun MenuScope.button(
    columns: IntRange,
    row: Int,
    glyphs: ButtonGlyphs,
    pressDuration: Duration = DEFAULT_BUTTON_PRESS_DURATION,
    block: ButtonScope.() -> Unit,
) {
    button(cell(columns, row), spanSlots(columns, row), glyphs, pressDuration, block) { index, slotBlock ->
        slot(index, slotBlock)
    }
}

/**
 * Adds a button over the slot at [column] and [row] of each viewer's own inventory. Rows 0 to 2 are the main
 * inventory from the top and row 3 is the hotbar. Viewers see the button instead of their own item while the menu is
 * open. See the `button` overload with a slot index for the rest.
 *
 * @throws IllegalArgumentException if the slot is outside 9 by 4, a glyph has no width, or [pressDuration] is negative.
 */
public fun MenuScope.playerButton(
    column: Int,
    row: Int,
    glyphs: ButtonGlyphs,
    pressDuration: Duration = DEFAULT_BUTTON_PRESS_DURATION,
    block: ButtonScope.() -> Unit,
) {
    playerButton(column..column, row, glyphs, pressDuration, block)
}

/**
 * Adds a button that spans the slots in [columns] of [row] of each viewer's own inventory, for glyphs wider than one
 * slot. See the `playerButton` overload with one column.
 */
public fun MenuScope.playerButton(
    columns: IntRange,
    row: Int,
    glyphs: ButtonGlyphs,
    pressDuration: Duration = DEFAULT_BUTTON_PRESS_DURATION,
    block: ButtonScope.() -> Unit,
) {
    button(playerCell(columns, row), columns.toList(), glyphs, pressDuration, block) { column, slotBlock ->
        playerSlot(column, row, slotBlock)
    }
}

private fun MenuScope.button(
    at: Position,
    slots: List<Int>,
    glyphs: ButtonGlyphs,
    pressDuration: Duration,
    block: ButtonScope.() -> Unit,
    // Fills one of [slots], which are slot indices for menu buttons and columns for player buttons
    fill: RenderScope.(slot: Int, block: SlotScope.() -> Unit) -> Unit,
) {
    require(!pressDuration.isNegative()) { "Press duration must not be negative, but was $pressDuration" }
    listOf(glyphs.normal, glyphs.pressed, glyphs.disabled).forEach(Glyph::requireWidth)

    val button = ButtonScope(session).apply(block)
    val isPressed = state(false)

    element {
        val enabled = button.enabled()
        val shown = when {
            isPressed.value -> glyphs.pressed
            !enabled -> glyphs.disabled
            else -> glyphs.normal
        }
        spanGlyph(shown, at)

        val tooltip = button.tooltip?.invoke()
        val hover = button.hover?.takeIf { enabled }?.let { hoverSprite(it, at, tooltip) }
        val invisible = invisibleItem(tooltip, hover)

        val clickable: SlotScope.() -> Unit = {
            item { invisible }
            onClick { click ->
                if (isPressed.value || !button.enabled()) {
                    button.onDisabledClick?.invoke(click)
                    return@onClick
                }

                if (pressDuration.isPositive()) {
                    isPressed.value = true
                    launch {
                        delay(pressDuration)
                        isPressed.value = false
                    }
                }
                button.onClick?.invoke(click)
            }
        }
        slots.forEach { fill(it, clickable) }
    }
}

@MenuDsl
public class ButtonScope internal constructor(
    public val session: MenuSession,
) {
    internal var tooltip: (() -> Component)? = null
    internal var enabled: () -> Boolean = { true }
    internal var onClick: ((ClickContext) -> Unit)? = null
    internal var onDisabledClick: ((ClickContext) -> Unit)? = null
    internal var hover: Glyph? = null

    /**
     * Shows the component [block] returns as the tooltip. Without it, the button has no tooltip.
     */
    public fun tooltip(block: () -> Component) {
        tooltip = block
    }

    /**
     * Shows [glyph] over the button while the mouse is over it. Disabled buttons show no sprite. [glyph] must come
     * from a sheet with shader data rows, with its width and height excluding those rows, and your resource pack
     * needs matching text shaders. Without them, the sprite shows in the tooltip next to the mouse. With no
     * [tooltip], [MenuStyle.hoverTooltipStyle] hides the tooltip box.
     */
    public fun hover(glyph: Glyph) {
        glyph.requireWidth()
        hover = glyph
    }

    /**
     * Enables the button only while [condition] returns true. Enabled by default.
     */
    public fun enabled(condition: () -> Boolean) {
        enabled = condition
    }

    /**
     * Runs [handler] on the clicking player's tick thread when a player clicks the enabled button.
     */
    public fun onClick(handler: (ClickContext) -> Unit) {
        onClick = handler
    }

    /**
     * Runs [handler] when a player clicks the button while it is disabled or pressed.
     */
    public fun onDisabledClick(handler: (ClickContext) -> Unit) {
        onDisabledClick = handler
    }
}
