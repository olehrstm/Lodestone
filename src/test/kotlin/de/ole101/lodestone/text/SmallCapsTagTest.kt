package de.ole101.lodestone.text

import de.ole101.lodestone.testing.colors
import de.ole101.lodestone.testing.plain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import net.kyori.adventure.text.minimessage.MiniMessage

class SmallCapsTagTest : FunSpec({

    val miniMessage = MiniMessage.builder()
        .editTags { it.resolvers(ColorTag(), SmallCapsTag.RESOLVER) }
        .build()

    fun smallCaps(input: String): String = miniMessage.deserialize("<small>$input</small>").plain()

    context("translation") {
        test("converts lowercase letters") {
            smallCaps("hello") shouldBe "ʜᴇʟʟᴏ"
        }

        test("converts uppercase letters") {
            smallCaps("HELLO") shouldBe "ʜᴇʟʟᴏ"
        }

        test("converts the whole alphabet") {
            smallCaps("abcdefghijklmnopqrstuvwxyz") shouldBe "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"
        }

        test("leaves s and x as they are, since no small-caps glyph exists") {
            smallCaps("sx") shouldBe "sx"
        }

        test("leaves digits, whitespace and punctuation untouched") {
            smallCaps("a1 b-2!") shouldBe "ᴀ1 ʙ-2!"
        }

        test("handles an empty body") {
            smallCaps("") shouldBe ""
        }

        test("preserves length for any input") {
            checkAll(Arb.string(0..40, Codepoint.alphanumeric())) { input ->
                smallCaps(input).length shouldBe input.length
            }
        }

        test("changes nothing that is not a letter") {
            checkAll(Arb.string(0..40, Codepoint.alphanumeric())) { input ->
                val translated = smallCaps(input)

                input.indices.forEach { index ->
                    if (!input[index].isLetter()) translated[index] shouldBe input[index]
                }
            }
        }
    }

    context("composition") {
        test("applies to text nested inside other tags") {
            val component = miniMessage.deserialize("<small><!gold>hello</!gold></small>")

            component.plain() shouldBe "ʜᴇʟʟᴏ"
            component.colors() shouldContainExactly listOf(Colors.GOLD)
        }

        test("applies only inside its own range") {
            miniMessage.deserialize("<small>abc</small>def").plain() shouldBe "ᴀʙᴄdef"
        }

        test("survives being nested in itself") {
            miniMessage.deserialize("<small>ab<small>cd</small></small>").plain() shouldBe "ᴀʙᴄᴅ"
        }
    }
})
