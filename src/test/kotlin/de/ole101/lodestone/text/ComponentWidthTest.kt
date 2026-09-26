package de.ole101.lodestone.text

import de.ole101.lodestone.text.glyph.space
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration

class ComponentWidthTest : FunSpec({

    test("adds one pixel of spacing after each character") {
        Component.text("il").pixelWidth() shouldBe (1 + 1) + (2 + 1)
    }

    test("adds one pixel per character for bold text") {
        Component.text("il", null, TextDecoration.BOLD).pixelWidth() shouldBe (1 + 2) + (2 + 2)
    }

    test("inherits bold from the parent unless a child turns it off") {
        val component = "<b>i<!b>i</!b></b>i".mini()

        component.pixelWidth() shouldBe 3 + 2 + 2
    }

    test("measures small caps glyphs") {
        "<small>ai</small>".mini().pixelWidth() shouldBe (5 + 1) + (3 + 1)
    }

    test("uses the default width for unknown glyphs") {
        Component.text("Ω").pixelWidth() shouldBe 6
    }

    test("measures an empty component as zero") {
        Component.empty().pixelWidth() shouldBe 0
    }

    test("measures a space as its pixel count") {
        for (pixels in -8192..8192) space(pixels).pixelWidth() shouldBe pixels
    }

    test("adds spaces to the text width") {
        Component.text("ab").append(space(-12)).pixelWidth() shouldBe 0
    }

    test("measures other translatable components as zero") {
        Component.translatable("space.abc").pixelWidth() shouldBe 0
        Component.translatable("item.minecraft.stone").pixelWidth() shouldBe 0
    }
})
