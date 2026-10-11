package de.ole101.lodestone.text

import de.ole101.lodestone.text.glyph.SPACE_KEY_PREFIX
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration

/**
 * Returns the width of this component in pixels when rendered with the default Minecraft font.
 *
 * Every character except an ordinary space (`U+0020`) is followed by one pixel of spacing.
 * Ordinary spaces are 4 pixels wide, and ellipses (`…`) are 9 pixels wide including spacing.
 * Bold characters, including spaces, are one pixel wider.
 * Bold is inherited from parent components. Only text content is measured: translatable, keybind
 * and other non-text components count as zero width, but their children are still measured.
 * The exception is a translatable component with the key `space.<n>` from [de.ole101.lodestone.text.glyph.space],
 * which counts as `n` pixels.
 * Known glyph widths cover ASCII, additional font symbols, ellipses and the small caps glyphs from `<small>`.
 * Unlisted characters are assumed to be 5 pixels wide before spacing and bold are added.
 */
public fun Component.pixelWidth(): Int = pixelWidth(this, parentBold = false)

private fun pixelWidth(
    component: Component,
    parentBold: Boolean,
): Int {
    val bold = component.decoration(TextDecoration.BOLD).toBoolean(parentBold)
    val text = (component as? TextComponent)?.content().orEmpty()
    val ownWidth = text.codePoints().map { glyphWidth(it, bold) }.sum()

    return spaceWidth(component) + ownWidth + component.children().sumOf { pixelWidth(it, bold) }
}

private fun TextDecoration.State.toBoolean(default: Boolean): Boolean = when (this) {
    TextDecoration.State.TRUE -> true
    TextDecoration.State.FALSE -> false
    TextDecoration.State.NOT_SET -> default
}

private fun glyphWidth(
    codePoint: Int,
    bold: Boolean,
): Int {
    val spacing = if (codePoint == ' '.code) 0 else CHARACTER_SPACING
    return (GLYPH_WIDTHS[codePoint] ?: DEFAULT_GLYPH_WIDTH) + spacing + if (bold) 1 else 0
}

private fun spaceWidth(component: Component): Int = (component as? TranslatableComponent)
    ?.key()
    ?.removePrefix(SPACE_KEY_PREFIX)
    ?.toIntOrNull() ?: 0

private const val CHARACTER_SPACING = 1
private const val DEFAULT_GLYPH_WIDTH = 5

private val GLYPH_WIDTHS: Map<Int, Int> = buildMap {
    fun widths(
        width: Int,
        chars: String,
    ) = chars.codePoints().forEach { put(it, width) }

    widths(1, "!',.:;i|")
    widths(2, "`l")
    widths(3, "\"()*I[]t{}ɪ")
    widths(4, " <>fkªº▌⌡°ⁿ²")
    widths(5, "#$%&+-/0123456789=?ABCDEFGHIJKLMNOPQRSTUVWXYZ\\^_abcdeghjmnopqrsuvwxyz£ƒ¬│┤╡╕╛┐┘∈±≥≤÷∙■ᴀʙᴄᴅᴇғɢʜᴊᴋʟᴍɴᴏᴘǫʀᴛᴜᴠᴡʏᴢ")
    widths(6, "@~«»≡≈√")
    widths(7, "░╢╖╣║╗╝╜⌠∅")
    widths(8, "▒▓└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┌█▄▐▀…")
}
