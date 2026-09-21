package de.ole101.lodestone.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pattern
import io.kotest.property.checkAll
import net.kyori.adventure.text.format.TextColor

class ColorsTest : FunSpec({

    val names = Arb.of(Colors.names.toList())

    context("named") {
        test("resolves an exact lowercase name") {
            Colors.named("red") shouldBe Colors.RED
            Colors.named("cornflowerblue") shouldBe Colors.CORNFLOWER_BLUE
        }

        test("ignores case") {
            Colors.named("RED") shouldBe Colors.RED
            Colors.named("DarkOliveGreen") shouldBe Colors.DARK_OLIVE_GREEN
        }

        test("ignores separators and digits") {
            Colors.named("dark_blue") shouldBe Colors.DARK_BLUE
            Colors.named("dark-blue") shouldBe Colors.DARK_BLUE
            Colors.named("Dark Blue") shouldBe Colors.DARK_BLUE
            Colors.named("dark.blue.1") shouldBe Colors.DARK_BLUE
        }

        test("returns null for an unknown name") {
            Colors.named("burntsienna").shouldBeNull()
        }

        test("returns null when nothing is left after normalisation") {
            Colors.named("").shouldBeNull()
            Colors.named("___").shouldBeNull()
            Colors.named("42").shouldBeNull()
        }

        test("resolves every registered name") {
            checkAll(names) { name ->
                Colors.named(name) shouldBe Colors.byName.getValue(name)
            }
        }

        test("resolves every registered name regardless of case or separators") {
            checkAll(names, Arb.pattern("[ _.\\-0-9]{0,3}")) { name, noise ->
                val mangled = name.uppercase().toCharArray().joinToString(noise, prefix = noise, postfix = noise)

                Colors.named(mangled) shouldBe Colors.byName.getValue(name)
            }
        }
    }

    context("registry") {
        test("maps names to their hex value") {
            Colors.RED shouldBe TextColor.color(0xFF0000)
            Colors.BLACK shouldBe TextColor.color(0x000000)
            Colors.WHITE shouldBe TextColor.color(0xFFFFFF)
            Colors.REBECCA_PURPLE shouldBe TextColor.color(0x663399)
        }

        test("exposes the same entries through byName, names and values") {
            Colors.names shouldBe Colors.byName.keys
            Colors.values shouldContainExactly Colors.byName.values.toList()
            Colors.values shouldHaveSize Colors.byName.size
        }

        test("keeps registration order") {
            Colors.names.first() shouldBe "aliceblue"
            Colors.names.last() shouldBe "yellowgreen"
        }

        test("registers british spellings as aliases") {
            Colors.GRAY shouldBe Colors.GREY
            Colors.DARK_GRAY shouldBe Colors.DARK_GREY
            Colors.LIGHT_SLATE_GRAY shouldBe Colors.LIGHT_SLATE_GREY

            Colors.byName shouldContainKey "grey"
            Colors.byName shouldContainKey "gray"
        }

        test("holds every name in lowercase letters only") {
            Colors.names.forEach { name ->
                name shouldBe name.lowercase().filter { it.isLetter() }
            }
        }
    }
})
