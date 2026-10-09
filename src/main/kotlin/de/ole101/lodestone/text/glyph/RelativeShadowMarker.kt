package de.ole101.lodestone.text.glyph

import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

// Text in this color is hidden by the GUI text shader, which draws its shadow marker instead
internal val SHADER_HIDDEN_COLOR: TextColor = TextColor.color(0x4EB000)

private const val SENTINEL_ALPHA = 0x4E
private const val FEATURE_OFFSET = 0xB0
private const val Y_OFFSET_BIAS = 2048

/**
 * Encodes a relative vertical offset and RGB332 tint for the GUI text shader.
 *
 * Layout of the resulting ARGB value: `AA RR GG BB` = `4E`, `B0 | high 4 bits of yOffset`,
 * `low 8 bits of yOffset`, `RGB332 tint`. The offset is biased by 2048 so it is stored unsigned.
 */
internal fun buildRelativeShadowMarker(
    yOffset: Int,
    tint: TextColor,
): ShadowColor {
    require(yOffset in -Y_OFFSET_BIAS until Y_OFFSET_BIAS) { "yOffset out of range: $yOffset" }
    val biased = yOffset + Y_OFFSET_BIAS
    val rgb332 = (tint.red() and 0xE0) or ((tint.green() and 0xE0) shr 3) or (tint.blue() shr 6)

    val argb = (SENTINEL_ALPHA shl 24) or
        ((FEATURE_OFFSET or (biased shr 8)) shl 16) or
        ((biased and 0xFF) shl 8) or
        rgb332
    return ShadowColor.shadowColor(argb)
}
