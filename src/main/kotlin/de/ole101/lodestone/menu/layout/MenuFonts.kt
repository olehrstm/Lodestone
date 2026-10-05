package de.ole101.lodestone.menu.layout

import de.ole101.lodestone.text.glyph.Space

/** Fonts from the resource pack that menu titles use. The spacing font is [Space.font]. */
public object MenuFonts {

    /**
     * Id of a tooltip style from your resource pack that draws no box, for example `"myserver:hover"`. A button with
     * a hover sprite but no tooltip uses it, so only the sprite shows. Without one, the box shows as an empty tooltip.
     */
    @Volatile
    public var hoverTooltipStyle: String? = null
}
