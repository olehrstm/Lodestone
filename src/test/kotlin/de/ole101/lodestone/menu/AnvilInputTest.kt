package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.anvilInput
import de.ole101.lodestone.menu.element.overlay
import de.ole101.lodestone.menu.layout.px
import de.ole101.lodestone.reactive.state
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fakePlayer
import de.ole101.lodestone.text.glyph.Glyph
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.component.DataComponents
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.player.PlayerAnvilInputEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class AnvilInputTest : FunSpec({
    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
    }

    test("confirm uses the latest text without moving items or spending experience") {
        val player = fakePlayer()
        val confirmed = mutableListOf<String>()
        val session = anvilInput("initial") { onConfirm { _, text -> confirmed += text } }.open(player)
        val inventory = session.inventory
        session[0].get(DataComponents.CUSTOM_NAME) shouldBe Component.text("initial")
        session[0].get(DataComponents.REPAIR_COST)!! shouldBeLessThan 0
        val level = player.level
        EventDispatcher.call(PlayerAnvilInputEvent(player, inventory, "latest"))
        val click = InventoryPreClickEvent(inventory, player, Click.Left(2))
        EventDispatcher.call(click)
        confirmed shouldBe listOf("latest")
        click.isCancelled shouldBe true
        player.inventory.cursorItem.isAir shouldBe true
        player.level shouldBe level
        player.openInventory shouldBe null
        session.isDisposed shouldBe true
    }

    test("cancel returns to the parent without confirming") {
        val player = fakePlayer()
        val parent = menu(rows = 1) {}.open(player)
        var cancelled = 0
        var confirmed = 0
        val session = anvilInput {
            onCancel { cancelled++ }
            onConfirm { _, _ -> confirmed++ }
        }.open(player, back = parent)
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(0)))
        cancelled shouldBe 1
        confirmed shouldBe 0
        player.openInventory shouldBe parent.inventory
        session.isDisposed shouldBe true
        parent.dispose()
    }

    test("validation leaves the menu open and a handler may navigate elsewhere") {
        val player = fakePlayer()
        val destination = menu(rows = 1) {}.create()
        val session = anvilInput {
            accepts { it.isNotBlank() }
            onConfirm { viewer, _ -> destination.open(viewer) }
        }.open(player)
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(2)))
        player.openInventory shouldBe session.inventory
        EventDispatcher.call(PlayerAnvilInputEvent(player, session.inventory, "search"))
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(2)))
        player.openInventory shouldBe destination.inventory
        destination.dispose()
    }

    test("reactive title updates retain typed text") {
        val player = fakePlayer()
        val session = anvilInput("initial") {
            layout {
                title { Component.text(anvilInput) }
                overlay(Glyph(Key.key("test:background"), 'a', 176, 166), px(0, 0))
            }
            onConfirm { _, _ -> }
        }.open(player)
        EventDispatcher.call(PlayerAnvilInputEvent(player, session.inventory, "changed"))
        schedulerManager.processTickEnd()
        session[0].get(DataComponents.CUSTOM_NAME) shouldBe Component.text("changed")
        session.dispose()
    }

    test("a re-rendered cancel item keeps the typed text") {
        val player = fakePlayer()
        val amount = state(1)
        val session = anvilInput("initial") {
            cancelItem { ItemStack.of(Material.ARROW, amount.value) }
            onConfirm { _, _ -> }
        }.open(player)
        EventDispatcher.call(PlayerAnvilInputEvent(player, session.inventory, "typed"))
        amount.value = 2
        schedulerManager.processTickEnd()
        session[0].get(DataComponents.CUSTOM_NAME) shouldBe Component.text("typed")
        session.dispose()
    }

    test("the initial text is filtered and trimmed, but the field keeps what was typed") {
        val player = fakePlayer()
        val session = anvilInput("  start ") { onConfirm { _, _ -> } }.open(player)
        session.anvilInput shouldBe "start"
        EventDispatcher.call(PlayerAnvilInputEvent(player, session.inventory, "a "))
        session.anvilInput shouldBe "a"
        session.dispose()
    }

    test("typed text keeps printable ASCII only and is trimmed") {
        val player = fakePlayer()
        val session = anvilInput { onConfirm { _, _ -> } }.open(player)
        EventDispatcher.call(PlayerAnvilInputEvent(player, session.inventory, "  h\u00e9llo\u00a7 "))
        session.anvilInput shouldBe "hllo"
        session.dispose()
    }

    test("right clicks on the actions do nothing") {
        val player = fakePlayer()
        var confirmed = 0
        val session = anvilInput { onConfirm { _, _ -> confirmed++ } }.open(player)
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Right(2)))
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Right(0)))
        confirmed shouldBe 0
        player.openInventory shouldBe session.inventory
        session.dispose()
    }

    test("anvil input ignores other inventories and players and is isolated between sessions") {
        val player = fakePlayer()
        val other = fakePlayer()
        val blueprint = anvilInput("initial") { onConfirm { _, _ -> } }
        val first = blueprint.open(player)
        val second = blueprint.open(other)
        EventDispatcher.call(PlayerAnvilInputEvent(player, first.inventory, "first"))
        EventDispatcher.call(PlayerAnvilInputEvent(other, first.inventory, "spoof"))
        first.anvilInput shouldBe "first"
        second.anvilInput shouldBe "initial"
        first.dispose()
        second.dispose()
    }

    test("returning from a child retains input even without reactive title changes") {
        val player = fakePlayer()
        val input = anvilInput("initial") { onConfirm { _, _ -> } }.open(player)
        EventDispatcher.call(PlayerAnvilInputEvent(player, input.inventory, "retained"))
        val child = menu(rows = 1) {}.open(player, back = input)
        child.goBack(player) shouldBe true
        input[0].get(DataComponents.CUSTOM_NAME) shouldBe Component.text("retained")
        input.dispose()
    }

    test("input requires an anvil and a confirmation handler") {
        shouldThrow<IllegalStateException> { menu(rows = 1) { anvilInput { onConfirm { _, _ -> } } }.create() }
        shouldThrow<IllegalStateException> { anvilInput {}.create() }
    }
})
