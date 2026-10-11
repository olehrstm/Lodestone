package de.ole101.lodestone.text.glyph

import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

// Text in this color is hidden by the GUI text shader, which draws its shadow marker instead
internal val SHADER_HIDDEN_COLOR = TextColor.color(0x4EB000)

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

private const val HOVER_COORDINATE_OFFSET = 128
private const val HOVER_COORDINATE_MAX = 255
private const val HOVER_WIDTH_MAX = 2047

/**
 * Encodes a hover sprite for the GUI text shader, drawn [dx] and [dy] pixels from the screen center while a tooltip
 * of [tooltipWidth] pixels and [lines] lines is shown.
 *
 * Red and green are the offsets plus 128, blue and the top 3 bits of alpha are the tooltip width, clamped to
 * `0..2047`, and the low 5 bits of alpha are the number of lines. The shader draws the shadow at the target and cuts
 * out the part behind the tooltip.
 *
 * @throws IllegalArgumentException if [dx] or [dy] is outside `-128..127`.
 */
internal fun buildHoverShadow(
    dx: Int,
    dy: Int,
    tooltipWidth: Int,
    lines: Int,
): ShadowColor {
    val red = dx + HOVER_COORDINATE_OFFSET
    val green = dy + HOVER_COORDINATE_OFFSET
    require(red in 0..HOVER_COORDINATE_MAX && green in 0..HOVER_COORDINATE_MAX) {
        "Hover sprites must start within 128 pixels of the screen center, but were at $dx, $dy"
    }

    val width = tooltipWidth.coerceIn(0, HOVER_WIDTH_MAX)
    return ShadowColor.shadowColor(red, green, width and 0xFF, (width shr 8 shl 5) or lines)
}
