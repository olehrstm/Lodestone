package de.ole101.lodestone.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

class GlyphTest : FunSpec({

    test("renders the character in the glyph font") {
        val font = Key.key("lodestone:interface")

        Glyph(font, '\uE000').asComponent() shouldBe Component.text('\uE000').font(font)
    }
})
