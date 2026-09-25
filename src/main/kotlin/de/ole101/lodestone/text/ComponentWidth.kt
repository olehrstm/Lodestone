package de.ole101.lodestone.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration

/**
 * Returns the width of this component in pixels when rendered with the default Minecraft font.
 *
 * Every character is followed by one pixel of spacing, and bold characters are one pixel wider.
 * Bold is inherited from parent components. Only text content is measured: translatable, keybind
 * and other non-text components count as zero width, but their children are still measured.
 * Characters other than ASCII and the small caps glyphs from `<small>` are assumed to be 5 pixels wide.
 */
public fun Component.pixelWidth(): Int = pixelWidth(this, parentBold = false)

private fun pixelWidth(component: Component, parentBold: Boolean): Int {
    val bold = when (component.decoration(TextDecoration.BOLD)) {
        TextDecoration.State.TRUE -> true
        TextDecoration.State.FALSE -> false
        TextDecoration.State.NOT_SET -> parentBold
    }
    val text = (component as? TextComponent)?.content().orEmpty()
    val extraPerGlyph = if (bold) CHARACTER_SPACING + 1 else CHARACTER_SPACING
    val ownWidth = text.codePoints().map { (GLYPH_WIDTHS[it] ?: DEFAULT_GLYPH_WIDTH) + extraPerGlyph }.sum()

    return ownWidth + component.children().sumOf { pixelWidth(it, bold) }
}

private const val CHARACTER_SPACING = 1
private const val DEFAULT_GLYPH_WIDTH = 5

private val GLYPH_WIDTHS: Map<Int, Int> = mapOf(
    ' ' to 4, '!' to 1, '"' to 3, '#' to 5, '$' to 5, '%' to 5, '&' to 5, '\'' to 1,
    '(' to 3, ')' to 3, '*' to 3, '+' to 5, ',' to 1, '-' to 5, '.' to 1, '/' to 5,
    '0' to 5, '1' to 5, '2' to 5, '3' to 5, '4' to 5, '5' to 5, '6' to 5, '7' to 5,
    '8' to 5, '9' to 5, ':' to 1, ';' to 1, '<' to 4, '=' to 5, '>' to 4, '?' to 5,
    '@' to 6, 'A' to 5, 'B' to 5, 'C' to 5, 'D' to 5, 'E' to 5, 'F' to 5, 'G' to 5,
    'H' to 5, 'I' to 3, 'J' to 5, 'K' to 5, 'L' to 5, 'M' to 5, 'N' to 5, 'O' to 5,
    'P' to 5, 'Q' to 5, 'R' to 5, 'S' to 5, 'T' to 5, 'U' to 5, 'V' to 5, 'W' to 5,
    'X' to 5, 'Y' to 5, 'Z' to 5, '[' to 3, '\\' to 5, ']' to 3, '^' to 5, '_' to 5,
    '`' to 2, 'a' to 5, 'b' to 5, 'c' to 5, 'd' to 5, 'e' to 5, 'f' to 4, 'g' to 5,
    'h' to 5, 'i' to 1, 'j' to 5, 'k' to 4, 'l' to 2, 'm' to 5, 'n' to 5, 'o' to 5,
    'p' to 5, 'q' to 5, 'r' to 5, 's' to 5, 't' to 3, 'u' to 5, 'v' to 5, 'w' to 5,
    'x' to 5, 'y' to 5, 'z' to 5, '{' to 3, '|' to 1, '}' to 3, '~' to 6,
    'ᴀ' to 5, 'ʙ' to 5, 'ᴄ' to 5, 'ᴅ' to 5, 'ᴇ' to 5, 'ғ' to 5, 'ɢ' to 5, 'ʜ' to 5,
    'ɪ' to 3, 'ᴊ' to 5, 'ᴋ' to 5, 'ʟ' to 5, 'ᴍ' to 5, 'ɴ' to 5, 'ᴏ' to 5, 'ᴘ' to 5,
    'ǫ' to 5, 'ʀ' to 5, 'ᴛ' to 5, 'ᴜ' to 5, 'ᴠ' to 5, 'ᴡ' to 5, 'ʏ' to 5, 'ᴢ' to 5,
).mapKeys { it.key.code }
