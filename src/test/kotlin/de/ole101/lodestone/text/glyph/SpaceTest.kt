package de.ole101.lodestone.text.glyph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

class SpaceTest : FunSpec({

    afterEach { Space.font = Key.key("lodestone:space") }

    test("uses the space translation key in the lodestone space font") {
        space(-32) shouldBe Component.translatable("space.-32").font(Key.key("lodestone:space"))
    }

    test("uses the configured default font") {
        Space.font = Key.key("my:spaces")

        space(8) shouldBe Component.translatable("space.8").font(Key.key("my:spaces"))
    }

    test("returns an empty component for zero") {
        space(0) shouldBe Component.empty()
    }
})
