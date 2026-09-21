package de.ole101.lodestone.text

import de.ole101.lodestone.testing.colors
import de.ole101.lodestone.testing.plain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

class MiniMessageProviderTest : FunSpec({

    val default = MiniMessageProvider.instance

    afterEach { MiniMessageProvider.instance = default }

    context("the default instance") {
        test("parses built-in tags") {
            val component = MiniMessageProvider.parse("<red>hello")

            component.plain() shouldBe "hello"
            component.colors() shouldContainExactly listOf(NamedTextColor.RED)
        }

        test("parses the custom colour tag") {
            MiniMessageProvider.parse("<!cornflowerblue>hello").colors() shouldContainExactly
                listOf(Colors.CORNFLOWER_BLUE)
        }

        test("parses the small caps tag") {
            MiniMessageProvider.parse("<small>hello</small>").plain() shouldBe "ʜᴇʟʟᴏ"
        }

        test("parses both custom tags at once") {
            val component = MiniMessageProvider.parse("<small><!gold>hello</!gold></small>")

            component.plain() shouldBe "ʜᴇʟʟᴏ"
            component.colors() shouldContainExactly listOf(Colors.GOLD)
        }
    }

    context("a replaced instance") {
        test("is used for subsequent parsing") {
            MiniMessageProvider.instance = MiniMessage.builder().tags(TagResolver.empty()).build()

            MiniMessageProvider.parse("<red>hello").plain() shouldBe "<red>hello"
        }

    }
})
