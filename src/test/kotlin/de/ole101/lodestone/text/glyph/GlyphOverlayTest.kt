package de.ole101.lodestone.text.glyph

import de.ole101.lodestone.text.pixelWidth
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent

class GlyphOverlayTest : FunSpec({

    val shop = Glyph(Key.key("lodestone:gui"), '\uE000', 176)
    val shifted = ShiftedFonts { base, y -> Key.key(base.namespace(), "${base.value()}_y$y") }

    fun Component.spaceKeys() = children().filterIsInstance<TranslatableComponent>().map { it.key() }
    fun Component.items() = children().filterNot { it is TranslatableComponent || it == Component.empty() }

    test("returns an empty component when nothing is added") {
        glyphOverlay { } shouldBe Component.empty()
    }

    test("composes a glyph and centered text") {
        val line = glyphOverlay {
            glyph(shop, x = 0)
            text(Component.text("Shop"), x = 88, align = TextAlign.CENTER)
        }

        line.spaceKeys() shouldContainExactly listOf("space.-101", "space.-100")
        line.items() shouldContainExactly listOf(shop.asComponent(), Component.text("Shop"))
    }

    test("starts centered text at x minus half its width") {
        glyphOverlay { text(Component.text("ab"), x = 20, align = TextAlign.CENTER) }
            .spaceKeys() shouldContainExactly listOf("space.14", "space.-26")
    }

    test("starts right aligned text at x minus its width") {
        glyphOverlay { text(Component.text("ab"), x = 20, align = TextAlign.RIGHT) }
            .spaceKeys() shouldContainExactly listOf("space.8", "space.-20")
    }

    test("moves left for negative x") {
        val line = glyphOverlay { text(Component.text("ab"), x = -30) }

        line.spaceKeys() shouldContainExactly listOf("space.-30", "space.18")
        line.pixelWidth() shouldBe 0
    }

    test("keeps overlapping items in the order they were added") {
        val line = glyphOverlay {
            text(Component.text("b"), x = 0)
            text(Component.text("a"), x = 0)
        }

        line.items() shouldContainExactly listOf(Component.text("b"), Component.text("a"))
        line.spaceKeys() shouldContainExactly listOf("space.-6", "space.-6")
    }

    test("shifts the root and explicit child fonts for y other than 0") {
        val text = Component.text("a")
            .append(Component.text("b").font(Key.key("my:icons")))
            .append(Component.text("c"))

        val line = glyphOverlay(shifted) { text(text, x = 4, y = 12) }

        line.items() shouldContainExactly listOf(
            Component.text("a").font(Key.key("minecraft:default_y12"))
                .append(Component.text("b").font(Key.key("my:icons_y12")))
                .append(Component.text("c")),
        )
        line.children().filterIsInstance<TranslatableComponent>().map { it.font() } shouldContainExactly
            listOf(Space.font, Space.font)
    }

    test("throws for y other than 0 without shifted fonts") {
        shouldThrow<IllegalStateException> {
            glyphOverlay { text(Component.text("a"), x = 0, y = 12) }
        }.message shouldContain "12"
    }

    test("rejects a glyph without width") {
        shouldThrow<IllegalArgumentException> {
            glyphOverlay { glyph(Glyph(Key.key("lodestone:gui"), '\uE000'), x = 0) }
        }.message shouldContain "width"
    }

    test("text-only lines move the cursor by 0") {
        val item = Arb.string(0..10, Codepoint.alphanumeric())
        checkAll(Arb.list(item, 0..5), Arb.int(-200..200), Arb.enum<TextAlign>()) { texts, x, align ->
            glyphOverlay {
                texts.forEachIndexed { i, text -> text(Component.text(text), x + i * 7, align = align) }
            }.pixelWidth() shouldBe 0
        }
    }
})
