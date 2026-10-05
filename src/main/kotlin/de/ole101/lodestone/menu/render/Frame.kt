package de.ole101.lodestone.menu.render

import de.ole101.lodestone.menu.layout.MenuMetrics
import de.ole101.lodestone.menu.layout.MenuType
import de.ole101.lodestone.menu.layout.titleX
import de.ole101.lodestone.menu.slot.SlotView
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
import de.ole101.lodestone.text.glyph.glyphOverlay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

internal sealed interface TitleLayer {
    val x: Int
    val y: Int

    data class GlyphLayer(val glyph: Glyph, override val x: Int, override val y: Int, val color: TextColor) : TitleLayer
    data class TextLayer(val text: Component, override val x: Int, override val y: Int, val align: TextAlign, val onTop: Boolean = false) : TitleLayer
}

internal class Frame(val slots: Map<Int, SlotView>, val layers: List<TitleLayer>) {

    fun title(type: MenuType): Component = glyphOverlay {
        for (layer in layers) {
            val x = layer.x - type.titleX
            val y = layer.y - MenuMetrics.TITLE_Y
            when (layer) {
                // A glyph with a smaller ascent draws lower than text, so it moves up by the difference
                is TitleLayer.GlyphLayer -> shaderGlyph(layer.glyph, x, y + layer.glyph.ascent - Glyph.DEFAULT_ASCENT, layer.color)
                is TitleLayer.TextLayer -> shaderText(layer.text, x, y, layer.align)
            }
        }
    }

    companion object {
        val EMPTY: Frame = Frame(emptyMap(), emptyList())
    }
}

/** The ordered output of the elements and blocks of one menu scope. */
internal class Region {
    val entries = mutableListOf<Entry>()

    sealed interface Entry

    class ElementEntry : Entry {
        var slots: Map<Int, SlotView> = emptyMap()
        var layers: List<TitleLayer> = emptyList()
    }

    class BlockEntry : Entry {
        var regions: List<Region> = emptyList()
    }

    /** Builds a frame in declaration order. Later slots win, later layers draw on top, and the title draws last. */
    fun frame(): Frame {
        val slots = HashMap<Int, SlotView>()
        val layers = mutableListOf<TitleLayer>()

        fun collect(region: Region) {
            for (entry in region.entries) {
                when (entry) {
                    is ElementEntry -> {
                        slots += entry.slots
                        layers += entry.layers
                    }

                    is BlockEntry -> entry.regions.forEach(::collect)
                }
            }
        }

        collect(this)
        val (top, rest) = layers.partition { it is TitleLayer.TextLayer && it.onTop }
        return Frame(slots, rest + top)
    }
}
