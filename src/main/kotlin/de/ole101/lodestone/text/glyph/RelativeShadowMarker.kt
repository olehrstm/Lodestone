package de.ole101.lodestone.text.glyph

import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

// Text in this color is hidden by the GUI text shader, which draws its shadow marker instead
internal val SHADER_HIDDEN_COLOR: TextColor = TextColor.color(0x4EB000)

// The vanilla color of container titles
internal val TITLE_COLOR: TextColor = TextColor.color(0x404040)

private const val SENTINEL_ALPHA = 0x4E
private const val FEATURE_OFFSET = 0xB0

/** Encodes a relative vertical offset and RGB332 tint for the GUI text shader. */
internal fun buildRelativeShadowMarker(yOffset: Int, tint: TextColor): ShadowColor {
    require(yOffset in -2048..2047) { "yOffset out of range: $yOffset" }
    val biased = yOffset + 2048
    val rgb332 = ((tint.red() shr 5) shl 5) or ((tint.green() shr 5) shl 2) or (tint.blue() shr 6)
    return ShadowColor.shadowColor(
        (SENTINEL_ALPHA shl 24) or
                ((FEATURE_OFFSET or (biased shr 8)) shl 16) or
                ((biased and 0xFF) shl 8) or rgb332,
    )
}
