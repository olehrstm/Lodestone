package de.ole101.lodestone.sign

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SignInputTest : FunSpec({

    test("defaults to an empty first input line with a hint below") {
        val input = signInput { onInput { _, _ -> } }

        input.lines shouldBe listOf("", "", "^^^^^^^^^^^^^^^", "Enter query")
        input.inputLines shouldBe 0..1
    }

    test("requires an onInput handler") {
        shouldThrow<IllegalStateException> { signInput { } }
    }

    test("requires exactly four lines") {
        shouldThrow<IllegalArgumentException> {
            signInput {
                lines = listOf("", "")
                onInput { _, _ -> }
            }
        }
    }

    test("requires input lines within the sign") {
        shouldThrow<IllegalArgumentException> {
            signInput {
                inputLines = 2..4
                onInput { _, _ -> }
            }
        }
    }
})
