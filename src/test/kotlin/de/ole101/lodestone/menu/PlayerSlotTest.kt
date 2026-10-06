package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.playerSlot
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fakePlayer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class PlayerSlotTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
    }

    test("player slots cover the viewer's items in the window without changing them") {
        val player = fakePlayer()
        player.inventory.setItemStack(0, ItemStack.of(Material.STONE))
        player.inventory.setItemStack(9, ItemStack.of(Material.DIRT))
        val session = menu(rows = 1) {
            playerSlot(0, 0) { item { ItemStack.of(Material.DIAMOND) } }
            playerSlot(4, 3) { item { ItemStack.of(Material.EMERALD) } }
        }.open(player)

        val items = session.playerWindowItems(player)
        // Window 0 puts the main inventory at 9 to 35 and the hotbar at 36 to 44
        items[9] shouldBe ItemStack.of(Material.DIAMOND)
        items[40] shouldBe ItemStack.of(Material.EMERALD)
        items[36] shouldBe ItemStack.of(Material.STONE)
        player.inventory.getItemStack(9) shouldBe ItemStack.of(Material.DIRT)
        session.dispose()
    }

    test("a click on a player slot reaches its handler and moves nothing") {
        val player = fakePlayer()
        player.inventory.setItemStack(4, ItemStack.of(Material.STONE))
        val clicked = mutableListOf<Int>()
        val session = menu(rows = 1) {
            playerSlot(4, 3) { onClick { clicked += it.slot } }
        }.open(player)

        val click = InventoryPreClickEvent(player.inventory, player, Click.Left(4))
        EventDispatcher.call(click)

        clicked shouldBe listOf(31)
        click.isCancelled shouldBe true
        player.inventory.getItemStack(4) shouldBe ItemStack.of(Material.STONE)
        session.dispose()
    }

    test("player slots outside 9 by 4 are rejected") {
        shouldThrow<IllegalArgumentException> { menu(rows = 1) { playerSlot(0, 4) {} }.create() }
        shouldThrow<IllegalArgumentException> { menu(rows = 1) { playerSlot(9, 0) {} }.create() }
    }
})
