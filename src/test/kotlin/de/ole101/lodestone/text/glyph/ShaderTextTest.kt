package de.ole101.lodestone.text.glyph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.text.format.NamedTextColor

class ShaderTextTest : FunSpec({
    test("positions text without shifted fonts and preserves inherited and explicit tints") {
        val font = Key.key("test:font")
        val source = Component.text("root", NamedTextColor.RED).font(font)
            .append(Component.text("inherited"))
            .append(Component.text("blue", NamedTextColor.BLUE))
        val line = glyphOverlay { shaderText(source, 0, 12, TextAlign.LEFT) }
        val root = line.children().filterIsInstance<TextComponent>().first { it.content() == "root" }
        root.font() shouldBe font
        root.shadowColor() shouldBe buildRelativeShadowMarker(12, NamedTextColor.RED)
        root.children()[0].shadowColor() shouldBe root.shadowColor()
        root.children()[1].shadowColor() shouldBe buildRelativeShadowMarker(12, NamedTextColor.BLUE)
    }

    test("marks translation component arguments and keeps numeric arguments") {
        val source = Component.translatable("test").arguments(
            Component.text("value", NamedTextColor.GREEN), TranslationArgument.numeric(42),
        )
        val line = glyphOverlay { shaderText(source, 0, -10, TextAlign.CENTER) }
        val translated = line.children().filterIsInstance<TranslatableComponent>().first { it.key() == "test" }
        val argument = translated.arguments()[0].value() as Component
        argument.shadowColor() shouldBe buildRelativeShadowMarker(-10, NamedTextColor.GREEN)
        translated.arguments()[1].value() shouldBe 42
    }
})
