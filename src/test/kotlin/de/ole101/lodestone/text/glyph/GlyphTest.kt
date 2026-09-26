package de.ole101.lodestone.text.glyph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

class GlyphTest : FunSpec({

    test("renders the character in the glyph font") {
        val font = Key.key("lodestone:interface")

        Glyph(font, '\uE000').asComponent() shouldBe Component.text('\uE000').font(font)
    }

    test("rejects a negative width") {
        shouldThrow<IllegalArgumentException> { Glyph(Key.key("lodestone:gui"), '\uE000', -1) }.message shouldContain "-1"
    }
})
