package de.ole101.lodestone.text

import de.ole101.lodestone.testing.RecordingAudience
import de.ole101.lodestone.testing.colors
import de.ole101.lodestone.testing.plain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component

class MiniMessageExtensionsTest : FunSpec({

    context("mini") {
        test("applies the custom tags") {
            val component = "<small><!gold>hello</!gold></small>".mini()

            component.plain() shouldBe "ʜᴇʟʟᴏ"
            component.colors() shouldContainExactly listOf(Colors.GOLD)
        }
    }

    context("Audience.sendMini") {
        test("sends the parsed message") {
            val audience = RecordingAudience()

            audience.sendMini("<red>hello")

            audience.received shouldContainExactly listOf("<red>hello".mini())
        }
    }

    context("String.sendMini") {
        test("sends the parsed message to every audience") {
            val first = RecordingAudience()
            val second = RecordingAudience()

            "<red>hello".sendMini(first, second)

            first.received shouldContainExactly listOf("<red>hello".mini())
            second.received shouldContainExactly listOf("<red>hello".mini())
        }

        test("parses once and shares the component") {
            val first = RecordingAudience()
            val second = RecordingAudience()

            "<red>hello".sendMini(first, second)

            (first.received.single() === second.received.single()) shouldBe true
        }

    }

    context("Component.send") {
        test("sends the same component to every audience") {
            val component = Component.text("hello")
            val first = RecordingAudience()
            val second = RecordingAudience()

            component.send(first, second)

            (first.received.single() === component) shouldBe true
            (second.received.single() === component) shouldBe true
        }

        test("sends once per audience, in order") {
            val audience = RecordingAudience()

            Component.text("one").send(audience)
            Component.text("two").send(audience)

            audience.received shouldContainExactly listOf(Component.text("one"), Component.text("two"))
        }

    }
})
