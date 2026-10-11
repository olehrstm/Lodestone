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
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

class GlyphOverlayTest :
    FunSpec({

        val shop = Glyph(Key.key("lodestone:gui"), '\uE000', 176)

        fun Component.spaceKeys() = children().filterIsInstance<TranslatableComponent>().map { it.key() }

        fun Component.items() = children().filterNot { it is TranslatableComponent || it == Component.empty() }

        test("returns an empty component when nothing is added") {
            glyphOverlay { } shouldBe Component.empty()
        }

        test("moves a shifted glyph with the shader and keeps its tint") {
            val line = glyphOverlay { glyph(shop, x = 0, y = 4, color = NamedTextColor.WHITE) }

            line.items().single().color() shouldBe SHADER_HIDDEN_COLOR
            line.items().single().shadowColor() shouldBe buildRelativeShadowMarker(4, NamedTextColor.WHITE)
            line.items().single().font() shouldBe shop.font
        }

        test("tints an uncolored shifted glyph white by default") {
            val line = glyphOverlay { glyph(shop, x = 0, y = -3) }

            line.items().single().shadowColor() shouldBe buildRelativeShadowMarker(-3, NamedTextColor.WHITE)
        }

        test("tints uncolored shifted items with the inherited color") {
            val gray = TextColor.color(0x404040)
            val line =
                glyphOverlay(gray) {
                    glyph(shop, x = 0, y = 5)
                    text(Component.text("a"), x = 0, y = 5)
                    text(Component.text("b", NamedTextColor.RED), x = 0, y = 5)
                }

            line.items().map { it.shadowColor() } shouldContainExactly
                listOf(
                    buildRelativeShadowMarker(5, gray),
                    buildRelativeShadowMarker(5, gray),
                    buildRelativeShadowMarker(5, NamedTextColor.RED),
                )
        }

        test("leaves unshifted items without a shader marker") {
            val line = glyphOverlay { glyph(shop, x = 0, y = 0, color = NamedTextColor.WHITE) }

            line.items().single().shadowColor() shouldBe null
        }

        test("composes a glyph and centered text") {
            val line =
                glyphOverlay {
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
            val line =
                glyphOverlay {
                    text(Component.text("b"), x = 0)
                    text(Component.text("a"), x = 0)
                }

            line.items() shouldContainExactly listOf(Component.text("b"), Component.text("a"))
            line.spaceKeys() shouldContainExactly listOf("space.-6", "space.-6")
        }

        test("moves shifted text with the shader and keeps its fonts") {
            val text =
                Component
                    .text("a")
                    .append(Component.text("b").font(Key.key("my:icons")))

            val line = glyphOverlay { text(text, x = 4, y = 12) }

            val item = line.items().single()
            item.font() shouldBe null
            item.shadowColor() shouldBe buildRelativeShadowMarker(12, NamedTextColor.WHITE)
            item.children().single().font() shouldBe Key.key("my:icons")
            line.pixelWidth() shouldBe 0
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
