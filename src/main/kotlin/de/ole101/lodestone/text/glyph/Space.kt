package de.ole101.lodestone.text.glyph

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

internal const val SPACE_KEY_PREFIX = "space."

public object Space {

    /**
     * Font that [space] uses. Defaults to `lodestone:space`.
     */
    @Volatile
    public var font: Key = Key.key("lodestone:space")
}

/**
 * Returns a component that moves the text cursor by [pixels], where negative values move it left.
 *
 * It uses the resource pack translation key `space.<pixels>`, rendered in the spacing font [Space.font]. It returns an empty component for 0.
 * Keys the resource pack does not define show up as raw text on the client.
 *
 * @see glyphOverlay
 */
public fun space(pixels: Int): Component = if (pixels == 0) Component.empty() else Component.translatable("$SPACE_KEY_PREFIX$pixels").font(Space.font)
