package de.ole101.lodestone.command

import de.ole101.lodestone.testing.AdminSender
import de.ole101.lodestone.testing.TestSender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType

private fun kommand(name: String = "test", vararg aliases: String, block: Kommand.() -> Unit): Kommand =
    object : Kommand(name, *aliases) {}.apply(block)

private fun commandContext(input: String = "test", arguments: Map<String, Any> = emptyMap()): CommandContext =
    CommandContext(input).apply { map.putAll(arguments) }

class KommandTest : FunSpec({

    context("naming") {
        test("takes its name and aliases from the constructor") {
            val command = kommand("teleport", "tp", "tele") { }

            command.name shouldBe "teleport"
            command.aliases.toList() shouldContainExactly listOf("tp", "tele")
        }
    }

    context("defaultExecutor") {
        test("runs for a matching sender") {
            var ran = false

            val command = kommand { defaultExecutor<TestSender> { ran = true } }
            command.defaultExecutor.shouldNotBeNull().apply(TestSender(), commandContext())

            ran.shouldBeTrue()
        }

        test("passes the narrowed sender to the block") {
            val sender = AdminSender()
            var seen: CommandSender? = null

            val command = kommand { defaultExecutor<AdminSender> { seen = it } }
            command.defaultExecutor!!.apply(sender, commandContext())

            (seen === sender).shouldBeTrue()
        }

        test("runs for a subtype of the declared sender") {
            var ran = false

            val command = kommand { defaultExecutor<TestSender> { ran = true } }
            command.defaultExecutor!!.apply(AdminSender(), commandContext())

            ran.shouldBeTrue()
        }

        test("stays silent for a sender of the wrong type") {
            var ran = false

            val command = kommand { defaultExecutor<AdminSender> { ran = true } }
            command.defaultExecutor!!.apply(TestSender(), commandContext())

            ran.shouldBeFalse()
        }

        test("narrows to Player when no type is given") {
            var ran = false

            val command = kommand { defaultExecutor { ran = true } }
            command.defaultExecutor!!.apply(TestSender(), commandContext())

            ran.shouldBeFalse()
        }

        test("exposes the context to the block") {
            var seen: String? = null

            val command = kommand { defaultExecutor<TestSender> { seen = context.input } }
            command.defaultExecutor!!.apply(TestSender(), commandContext("test arg"))

            seen shouldBe "test arg"
        }
    }

    context("condition") {
        test("delegates to the block for a matching sender") {
            val command = kommand { condition<TestSender> { _, _ -> true } }

            command.condition.shouldNotBeNull().canUse(TestSender(), "test").shouldBeTrue()
        }

        test("honours a block that refuses") {
            val command = kommand { condition<TestSender> { _, _ -> false } }

            command.condition!!.canUse(TestSender(), "test").shouldBeFalse()
        }

        test("refuses a sender of the wrong type without calling the block") {
            var called = false

            val command = kommand {
                condition<AdminSender> { _, _ ->
                    called = true
                    true
                }
            }

            command.condition!!.canUse(TestSender(), "test").shouldBeFalse()
            called.shouldBeFalse()
        }

        test("passes the raw command string through, null included") {
            var seen: String? = "unset"

            val command = kommand {
                condition<TestSender> { _, raw ->
                    seen = raw
                    true
                }
            }

            command.condition!!.canUse(TestSender(), null)

            seen.shouldBeNull()
        }

        test("narrows to Player when no type is given") {
            val command = kommand { condition { _, _ -> true } }

            command.condition!!.canUse(TestSender(), "test").shouldBeFalse()
        }

    }

    context("syntax") {
        test("registers a syntax with its arguments, in order") {
            val target = ArgumentType.String("target")
            val amount = ArgumentType.Integer("amount")

            val command = kommand { syntax<TestSender>(target, amount) { } }

            command.syntaxes shouldHaveSize 1
            command.syntaxes.single().arguments.toList() shouldContainExactly listOf(target, amount)
        }

        test("reads an argument out of the context") {
            val amount = ArgumentType.Integer("amount")
            var seen: Int? = null

            val command = kommand { syntax<TestSender>(amount) { seen = amount() } }
            command.syntaxes.single().executor.apply(TestSender(), commandContext(arguments = mapOf("amount" to 7)))

            seen shouldBe 7
        }

        test("reads several arguments out of the context") {
            val target = ArgumentType.String("target")
            val amount = ArgumentType.Integer("amount")
            var seen: Pair<String, Int>? = null

            val command = kommand { syntax<TestSender>(target, amount) { seen = target() to amount() } }
            command.syntaxes.single().executor.apply(
                TestSender(),
                commandContext(arguments = mapOf("target" to "ole", "amount" to 3)),
            )

            seen shouldBe ("ole" to 3)
        }

        test("stays silent for a sender of the wrong type") {
            var ran = false

            val command = kommand { syntax<AdminSender>(ArgumentType.Integer("amount")) { ran = true } }
            command.syntaxes.single().executor.apply(TestSender(), commandContext(arguments = mapOf("amount" to 1)))

            ran.shouldBeFalse()
        }

        test("narrows to Player when no type is given") {
            var ran = false

            val command = kommand { syntax(ArgumentType.Integer("amount")) { ran = true } }
            command.syntaxes.single().executor.apply(TestSender(), commandContext(arguments = mapOf("amount" to 1)))

            ran.shouldBeFalse()
        }

        test("registers one syntax per call") {
            val command = kommand {
                syntax<TestSender>(ArgumentType.Integer("a")) { }
                syntax<TestSender>(ArgumentType.String("b")) { }
            }

            command.syntaxes shouldHaveSize 2
        }
    }

    context("conditionalSyntax") {
        test("registers the syntax with its condition") {
            val command = kommand {
                conditionalSyntax<TestSender>(
                    ArgumentType.Integer("amount"),
                    condition = { _, _ -> true },
                ) { }
            }

            command.syntaxes.single().commandCondition.shouldNotBeNull()
                .canUse(TestSender(), "test").shouldBeTrue()
        }

        test("refuses a sender of the wrong type") {
            val command = kommand {
                conditionalSyntax<AdminSender>(
                    ArgumentType.Integer("amount"),
                    condition = { _, _ -> true },
                ) { }
            }

            command.syntaxes.single().commandCondition!!.canUse(TestSender(), "test").shouldBeFalse()
        }

        test("keeps the condition and the executor independent") {
            var ran = false

            val command = kommand {
                conditionalSyntax<TestSender>(
                    ArgumentType.Integer("amount"),
                    condition = { _, _ -> false },
                ) { ran = true }
            }
            val syntax = command.syntaxes.single()

            syntax.commandCondition!!.canUse(TestSender(), "test").shouldBeFalse()
            syntax.executor.apply(TestSender(), commandContext(arguments = mapOf("amount" to 1)))

            ran.shouldBeTrue()
        }
    }

    context("subkommand") {
        test("registers a subcommand under the given name") {
            val command = kommand { subkommand("add") { } }

            command.subcommands shouldHaveSize 1
            command.subcommands.single().name shouldBe "add"
        }

        test("passes aliases through") {
            val command = kommand { subkommand("add", "plus", "insert") { } }

            command.subcommands.single().aliases.toList() shouldContainExactly listOf("plus", "insert")
        }

        test("applies the block to the subcommand, not the parent") {
            val command = kommand { subkommand("add") { defaultExecutor<TestSender> { } } }

            command.defaultExecutor.shouldBeNull()
            command.subcommands.single().defaultExecutor.shouldNotBeNull()
        }

        test("nests to any depth") {
            val command = kommand { subkommand("add") { subkommand("item") { } } }

            command.subcommands.single().subcommands.single().name shouldBe "item"
        }

        test("registers one subcommand per call, in order") {
            val command = kommand {
                subkommand("add") { }
                subkommand("remove") { }
            }

            command.subcommands.map { it.name } shouldContainExactly listOf("add", "remove")
        }
    }
})
