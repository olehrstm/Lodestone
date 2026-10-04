package de.ole101.lodestone.menu.layout

import de.ole101.lodestone.text.glyph.ShiftedFonts
import de.ole101.lodestone.text.glyph.Space

/** Fonts from the resource pack that menu titles use. The spacing font is [Space.font]. */
public object MenuFonts {

    /**
     * Fonts that draw text and glyphs away from the title line. Menus that only draw at the title height
     * work with the default [ShiftedFonts.None]. Anything else throws when the title is built.
     */
    @Volatile
    public var shiftedFonts: ShiftedFonts = ShiftedFonts.None

    /**
     * Id of a tooltip style from your resource pack that draws no box, for example `"myserver:hover"`. A button with
     * a hover sprite but no tooltip uses it, so only the sprite shows. Without one, the box shows as an empty tooltip.
     */
    @Volatile
    public var hoverTooltipStyle: String? = null
}
