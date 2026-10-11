package de.ole101.lodestone.command

import de.ole101.lodestone.LodestoneDsl
import de.ole101.lodestone.commandManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.condition.CommandCondition

/**
 * Minestom [Command] with a Kotlin DSL for executors, conditions, syntaxes and subcommands.
 *
 * Extend it, configure it via an `init` block, then call [register]. The command is reachable by
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
@LodestoneDsl
public abstract class Kommand(
    name: String,
    vararg aliases: String,
) : Command(name, *aliases) {
    @LodestoneDsl
    public class CommandScope internal constructor(
        public val context: CommandContext,
    ) {
        /**
         * Returns the value parsed for this argument. For an optional argument with no default that the input
         * left out, Minestom returns `null` here despite the non-null type, so use [invokeOrNull] for those.
         */
        public operator fun <T> Argument<T>.invoke(): T = context.get(this)

        /**
         * Returns the value parsed for this argument, or `null` when the input left it out and it has no default.
         */
        public fun <T> Argument<T>.invokeOrNull(): T? = context.get(this)
    }

    /**
     * Sets the executor that runs when the command is used without a matching syntax, for example with no
     * arguments. [block] receives the sender as `S`. Senders that are not an `S` are ignored and nothing runs.
     */
    public inline fun <reified S : CommandSender> defaultExecutor(noinline block: CommandScope.(S) -> Unit) {
        defaultExecutor = executorFor(S::class.java, block)
    }

    /**
     * Sets the condition that decides whether a sender can use the whole command.
     *
     * The block gets the sender and the raw command string. The string is `null` when Minestom only
     * checks whether to show the command in tab completion. Senders that are not an `S` always fail.
     */
    public inline fun <reified S : CommandSender> condition(noinline block: (S, String?) -> Boolean) {
        condition = conditionFor(S::class.java, block)
    }

    /**
     * Adds a syntax made of [arguments], in order, and runs [block] when the input matches them. Read the
     * parsed values inside [block] by calling the argument, for example `name()`. Senders that are not an `S`
     * are ignored and nothing runs.
     */
    public inline fun <reified S : CommandSender> syntax(
        vararg arguments: Argument<*>,
        noinline block: CommandScope.(S) -> Unit,
    ) {
        addSyntax(executorFor(S::class.java, block), *arguments)
    }

    /**
     * Like [syntax], but the syntax only applies when [condition] passes for the sender. The condition gets
     * the sender and the raw command string, which is `null` when Minestom only checks whether to show the
     * syntax in tab completion. Senders that are not an `S` always fail the condition.
     */
    public inline fun <reified S : CommandSender> conditionalSyntax(
        vararg arguments: Argument<*>,
        noinline condition: (S, String?) -> Boolean,
        noinline block: CommandScope.(S) -> Unit,
    ) {
        addConditionalSyntax(conditionFor(S::class.java, condition), executorFor(S::class.java, block), *arguments)
    }

    /**
     * Adds a subcommand called [name], also reachable by every entry in [aliases], and configures it in [block].
     * The subcommand is a full [Kommand], so it can have its own executors, conditions and subcommands.
     */
    public fun subkommand(
        name: String,
        vararg aliases: String,
        block: Kommand.() -> Unit,
    ) {
        addSubcommand(object : Kommand(name, *aliases) {}.apply(block))
    }

    /**
     * Registers this command with the server's command manager. Call it once, after the command is configured.
     */
    public fun register() {
        commandManager.register(this)
    }

    @PublishedApi
    internal fun <S : CommandSender> executorFor(
        type: Class<S>,
        block: CommandScope.(S) -> Unit,
    ): CommandExecutor = CommandExecutor { sender, context ->
        if (type.isInstance(sender)) {
            CommandScope(context).block(type.cast(sender))
        }
    }

    @PublishedApi
    internal fun <S : CommandSender> conditionFor(
        type: Class<S>,
        block: (S, String?) -> Boolean,
    ): CommandCondition = CommandCondition { sender, command ->
        type.isInstance(sender) && block(type.cast(sender), command)
    }
}
