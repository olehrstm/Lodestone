package de.ole101.lodestone.text.glyph

import net.kyori.adventure.key.Key

/**
 * Maps a font to its copy in the resource pack that is shifted a number of pixels down.
 *
 * Example: `ShiftedFonts { base, y -> Key.key(base.namespace(), "${base.value()}_y$y") }`.
 */
public fun interface ShiftedFonts {

    /**
     * Returns the font that draws the glyphs of [base] shifted [y] pixels down, where negative means up.
     * It is only called for a [y] other than 0, on the thread that calls [glyphOverlay].
     */
    public fun font(base: Key, y: Int): Key

    public companion object {
        public val None: ShiftedFonts = ShiftedFonts { _, y -> error("No shifted fonts configured, but an item uses y=$y") }
    }
}
