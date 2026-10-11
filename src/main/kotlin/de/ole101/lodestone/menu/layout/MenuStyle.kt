package de.ole101.lodestone.menu.layout

import de.ole101.lodestone.text.glyph.Space

/**
 * Resource pack styles that menus use. The spacing font is set with [Space.configure].
 */
public object MenuStyle {
    /**
     * Id of a tooltip style from the resource pack that draws no box, for example `"myserver:hover"`. A button with
     * a hover sprite but no tooltip uses it, so only the sprite shows. Without one, the box shows as an empty tooltip.
     */
    @Volatile
    public var hoverTooltipStyle: String? = null
        private set

    /**
     * Sets the hover tooltip style.
     */
    public fun configure(hoverTooltipStyle: String?) {
        this.hoverTooltipStyle = hoverTooltipStyle
    }
}
