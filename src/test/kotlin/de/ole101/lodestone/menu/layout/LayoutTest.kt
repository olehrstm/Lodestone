package de.ole101.lodestone.menu.layout

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LayoutTest : FunSpec({

    test("a rect lists its slots row by row") {
        SlotArea.rect(columns = 1..2, rows = 1..2).slotsIn(MenuType.CHEST_3) shouldBe listOf(10, 11, 19, 20)
    }

    test("cells and rects outside the grid are rejected") {
        shouldThrow<IllegalArgumentException> { cell(9, 0) }
        shouldThrow<IllegalArgumentException> { cell(0, -1) }
        shouldThrow<IllegalArgumentException> { SlotArea.rect(0..9, 0..0) }
    }
})
