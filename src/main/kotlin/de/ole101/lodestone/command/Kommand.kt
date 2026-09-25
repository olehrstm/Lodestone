package de.ole101.lodestone.command

import de.ole101.lodestone.commandManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.condition.CommandCondition
import net.minestom.server.entity.Player

@DslMarker
public annotation class KommandDsl

/**
 * Minestom [Command] with a Kotlin DSL for executors, conditions, syntaxes and subcommands.
 *
 * Extend it, configure it in an `init` block, then call [register]. The command is reachable by
 * [name] and every entry in [aliases].
 *
 * Executors and conditions are typed by sender. When the sender is not of the requested type,
 * an executor does nothing and a condition fails.
 *
 * ```
 * class HealKommand : Kommand("heal") {
 *     init {
 *         defaultExecutor { player -> player.health = 20f }
 *     }
 * }
 * ```
 */
@KommandDsl
public abstract class Kommand(name: String, vararg aliases: String) : Command(name, *aliases) {

    public class CommandScope(public val context: CommandContext) {
        /** Returns the value parsed for this argument, or `null` when the input left it out, and it has no default value. */
        public operator fun <T> Argument<T>.invoke(): T = context.get(this)
    }

    public inline fun <reified S : CommandSender> defaultExecutor(noinline block: CommandScope.(S) -> Unit) {
        defaultExecutor = executorFor(S::class.java, block)
    }

    @JvmName("defaultExecutorPlayer")
    public fun defaultExecutor(block: CommandScope.(Player) -> Unit): Unit = defaultExecutor<Player>(block)

    /**
     * Sets the condition that decides whether a sender can use the whole command.
     *
     * The block gets the sender and the raw command string. The string is `null` when Minestom only
     * checks whether to show the command in tab completion. Senders that are not an `S` always fail.
     */
    public inline fun <reified S : CommandSender> condition(noinline block: (S, String?) -> Boolean) {
        condition = conditionFor(S::class.java, block)
    }

    @JvmName("conditionPlayer")
    public fun condition(block: (Player, String?) -> Boolean): Unit = condition<Player>(block)

    public inline fun <reified S : CommandSender> syntax(
        vararg arguments: Argument<*>,
        noinline block: CommandScope.(S) -> Unit
    ) {
        addSyntax(executorFor(S::class.java, block), *arguments)
    }

    @JvmName("syntaxPlayer")
    public fun syntax(
        vararg arguments: Argument<*>,
        block: CommandScope.(Player) -> Unit
    ): Unit = syntax<Player>(*arguments, block = block)

    public inline fun <reified S : CommandSender> conditionalSyntax(
        vararg arguments: Argument<*>,
        noinline condition: (S, String?) -> Boolean,
        noinline block: CommandScope.(S) -> Unit
    ) {
        addConditionalSyntax(conditionFor(S::class.java, condition), executorFor(S::class.java, block), *arguments)
    }

    @JvmName("conditionalSyntaxPlayer")
    public fun conditionalSyntax(
        vararg arguments: Argument<*>,
        condition: (Player, String?) -> Boolean,
        block: CommandScope.(Player) -> Unit
    ): Unit = conditionalSyntax<Player>(*arguments, condition = condition, block = block)

    public fun subkommand(name: String, vararg aliases: String, block: Kommand.() -> Unit) {
        val subcommand = object : Kommand(name, *aliases) {}
        subcommand.block()
        addSubcommand(subcommand)
    }

    public fun register() {
        commandManager.register(this)
    }

    @PublishedApi
    internal fun <S : CommandSender> executorFor(type: Class<S>, block: CommandScope.(S) -> Unit): CommandExecutor {
        return CommandExecutor { sender, context ->
            if (type.isInstance(sender)) {
                CommandScope(context).block(type.cast(sender))
            }
        }
    }

    @PublishedApi
    internal fun <S : CommandSender> conditionFor(type: Class<S>, block: (S, String?) -> Boolean): CommandCondition {
        return CommandCondition { sender, command ->
            type.isInstance(sender) && block(type.cast(sender), command)
        }
    }
}
