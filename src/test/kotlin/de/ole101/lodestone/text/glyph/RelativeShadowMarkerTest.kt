package de.ole101.lodestone.text.glyph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

class RelativeShadowMarkerTest : FunSpec({
    test("encodes signed offset boundaries and RGB332 tint") {
        val tint = TextColor.color(0xA060C0)
        buildRelativeShadowMarker(-2048, tint) shouldBe ShadowColor.shadowColor(0x4EB000AF)
        buildRelativeShadowMarker(0, tint) shouldBe ShadowColor.shadowColor(0x4EB800AF)
        buildRelativeShadowMarker(2047, tint) shouldBe ShadowColor.shadowColor(0x4EBFFFAF)
    }

    test("rejects offsets outside the marker range") {
        shouldThrow<IllegalArgumentException> { buildRelativeShadowMarker(-2049, TextColor.color(0)) }
        shouldThrow<IllegalArgumentException> { buildRelativeShadowMarker(2048, TextColor.color(0)) }
    }
})
