package de.ole101.lodestone.menu.render

import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.slot.SlotView
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
import de.ole101.lodestone.text.glyph.glyphOverlay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.ItemStack

internal sealed interface TitleLayer {
    val x: Int
    val y: Int
    val onTop: Boolean
        get() = false

    data class GlyphLayer(
        val glyph: Glyph,
        override val x: Int,
        override val y: Int,
        val color: TextColor,
    ) : TitleLayer

    data class TextLayer(
        val text: Component,
        override val x: Int,
        override val y: Int,
        val align: TextAlign,
        override val onTop: Boolean = false,
    ) : TitleLayer
}

// The vanilla color of container titles
private val TITLE_COLOR = TextColor.color(0x404040)

internal class Frame(
    val slots: Map<Int, SlotView>,
    val layers: List<TitleLayer>,
    // Slots of the viewer's own inventory, by row-major index with the hotbar as row 3
    val playerSlots: Map<Int, SlotView> = emptyMap(),
) {
    val playerItems
        get() = playerSlots.mapValues { it.value.item }

    fun itemAt(slot: Int): ItemStack = slots[slot]?.item ?: ItemStack.AIR

    fun title(type: MenuType): Component = glyphOverlay(TITLE_COLOR) {
        for (layer in layers) {
            val x = layer.x - type.gui.titleX
            val y = layer.y - MenuMetrics.TITLE_Y
            when (layer) {
                // A glyph with a smaller ascent draws lower than text, so it moves up by the difference
                is TitleLayer.GlyphLayer -> glyph(layer.glyph, x, y + layer.glyph.ascent - Glyph.DEFAULT_ASCENT, layer.color)

                is TitleLayer.TextLayer -> text(layer.text, x, y, layer.align)
            }
        }
    }

    companion object {
        val EMPTY = Frame(emptyMap(), emptyList())
    }
}

/**
 * The ordered output of the elements and blocks of one menu scope.
 */
internal class Region {
    val entries = mutableListOf<Entry>()

    sealed interface Entry

    class ElementEntry : Entry {
        // The last render of the element, or null while it has none
        var output: RenderScope? = null
    }

    class BlockEntry : Entry {
        var regions: List<Region> = emptyList()
    }

    /**
     * Builds a frame in declaration order. Later slots win, later layers draw on top, and the title draws last.
     */
    fun frame(): Frame {
        val outputs = outputs()
        val (top, rest) = outputs.flatMap { it.layers }.partition { it.onTop }
        return Frame(
            slots = outputs.flatMap { it.slots.toList() }.toMap(),
            layers = rest + top,
            playerSlots = outputs.flatMap { it.playerSlots.toList() }.toMap(),
        )
    }

    private fun outputs(): List<RenderScope> = entries.flatMap { entry ->
        when (entry) {
            is ElementEntry -> listOfNotNull(entry.output)
            is BlockEntry -> entry.regions.flatMap(Region::outputs)
        }
    }
}
