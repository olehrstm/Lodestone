package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.prompt.promptSign
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fakePlayer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

// Test players are in no instance, so these drive the prompt lifecycle that promptSign uses directly
class PromptTest : FunSpec({

    beforeSpec { TestServer.init() }

    test("a prompt keeps the session and opens it again when it ends") {
        val player = fakePlayer()
        val session = menu(rows = 1) {}.open(player)

        val token = session.beginPrompt(player)
        session.viewers.shouldBeEmpty()
        session.isDisposed.shouldBeFalse()

        session.endPrompt(player, token)
        session.viewers shouldBe setOf(player)
        session.dispose()
    }

    test("an older prompt cannot end a newer one") {
        val player = fakePlayer()
        val session = menu(rows = 1) {}.open(player)

        val first = session.beginPrompt(player)
        val second = session.beginPrompt(player)
        session.endPrompt(player, first)
        session.viewers.shouldBeEmpty()

        session.endPrompt(player, second)
        session.viewers shouldBe setOf(player)
        session.dispose()
    }

    test("a player who leaves during a prompt releases the session") {
        val player = fakePlayer()
        val session = menu(rows = 1) {}.open(player)

        session.beginPrompt(player)
        player.playerConnection.disconnect()
        schedulerManager.processTick()

        session.isDisposed.shouldBeTrue()
    }

    test("a sign that cannot be opened opens the session again") {
        val player = fakePlayer()
        val session = menu(rows = 1) {}.open(player)

        // The player is in no instance
        shouldThrow<IllegalStateException> { session.promptSign(player) { onInput { _, _ -> } } }
        session.viewers shouldBe setOf(player)
        session.dispose()
    }
})
