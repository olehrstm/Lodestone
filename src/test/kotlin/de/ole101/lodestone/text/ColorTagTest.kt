package de.ole101.lodestone.text

import de.ole101.lodestone.testing.colors
import de.ole101.lodestone.testing.plain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage

class ColorTagTest : FunSpec({

    val resolver = ColorTag()
    val miniMessage = MiniMessage.builder()
        .editTags { it.resolver(resolver) }
        .build()

    context("has") {
        test("accepts a bang-prefixed colour name") {
            resolver.has("!red").shouldBeTrue()
            resolver.has("!cornflowerblue").shouldBeTrue()
        }

        test("rejects a colour name without the bang prefix") {
            resolver.has("red").shouldBeFalse()
        }

        test("rejects an unknown colour name") {
            resolver.has("!burntsienna").shouldBeFalse()
        }

        test("rejects the bare prefix") {
            resolver.has("!").shouldBeFalse()
        }

        test("accepts every registered colour name") {
            checkAll(Arb.of(Colors.byName.keys.toList())) { name ->
                resolver.has("!$name").shouldBeTrue()
            }
        }
    }

    context("resolve") {
        test("styles the text that follows the tag") {
            val component = miniMessage.deserialize("<!cornflowerblue>hello")

            component.plain() shouldBe "hello"
            component.colors() shouldContainExactly listOf(Colors.CORNFLOWER_BLUE)
        }

        test("styles only the text inside a closed tag") {
            val component = miniMessage.deserialize("plain<!gold>gilded</!gold>plain")

            component.plain() shouldBe "plaingildedplain"
            component.colors() shouldContainExactly listOf(Colors.GOLD)
        }

        test("leaves built-in colour tags working") {
            val component = miniMessage.deserialize("<red>hello")

            component.plain() shouldBe "hello"
            component.colors() shouldContainExactly listOf(NamedTextColor.RED)
        }

        test("gives the css colour where a built-in tag gives the minecraft one") {
            miniMessage.deserialize("<!red>hello").colors() shouldContainExactly listOf(Colors.RED)
            miniMessage.deserialize("<red>hello").colors() shouldContainExactly listOf(NamedTextColor.RED)

            Colors.RED shouldNotBe NamedTextColor.RED
        }

        test("resolves a name the built-in palette does not have") {
            miniMessage.deserialize("<!rebeccapurple>hello").colors() shouldContainExactly
                    listOf(Colors.REBECCA_PURPLE)
        }

        test("keeps an unknown bang tag as literal text") {
            val component = miniMessage.deserialize("<!burntsienna>hello")

            component.plain() shouldBe "<!burntsienna>hello"
            component.colors().shouldContainExactly(emptyList())
        }

        test("resolves every registered colour name") {
            checkAll(Arb.of(Colors.byName.keys.toList())) { name ->
                miniMessage.deserialize("<!$name>x").colors() shouldContainExactly
                        listOf(Colors.byName.getValue(name))
            }
        }
    }
})
