package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.slot
import de.ole101.lodestone.menu.element.text
import de.ole101.lodestone.menu.layout.px
import de.ole101.lodestone.menu.slot.SlotPolicy
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fakePlayer
import de.ole101.lodestone.testing.texts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.AbstractInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

private fun stack(material: Material, amount: Int = 1): ItemStack = ItemStack.of(material, amount)

private fun Player.click(inventory: AbstractInventory, click: Click): InventoryPreClickEvent =
    InventoryPreClickEvent(inventory, this, click).also(EventDispatcher::call)

class InputTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
    }

    // Slots 0 and 1 take diamonds, slot 2 is a locked diamond
    fun inputMenu(changes: MutableList<ItemStack> = mutableListOf()) = menu(rows = 1) {
        for (index in 0..1) {
            slot(index) {
                policy = SlotPolicy.PLACE
                accepts { it.material() == Material.DIAMOND }
                onChange { _, new -> changes += new }
            }
        }
        slot(2) { item { stack(Material.DIAMOND) } }
    }

    test("the session reads and replaces slot contents, and reads are tracked") {
        val changes = mutableListOf<ItemStack>()
        val session = menu(rows = 1) {
            slot(0) {
                policy = SlotPolicy.PLACE
                onChange { _, new -> changes += new }
            }
            text(px(8, 6)) { Component.text(session[0].amount()) }
        }.create()

        session[0] = stack(Material.DIAMOND, 3)
        session[0, 0] shouldBe stack(Material.DIAMOND, 3)
        schedulerManager.processTickEnd()

        session.inventory.title.texts() shouldBe "3"
        changes shouldContainExactly listOf(stack(Material.DIAMOND, 3))
        session.dispose()
    }

    test("a shift click from the player's inventory only fills accepting input slots") {
        val player = fakePlayer()
        val changes = mutableListOf<ItemStack>()
        val session = inputMenu(changes).open(player)
        player.inventory.setItemStack(9, stack(Material.DIAMOND, 70))
        player.inventory.setItemStack(10, stack(Material.STONE, 5))

        player.click(player.inventory, Click.LeftShift(9)).isCancelled.shouldBeTrue()
        player.click(player.inventory, Click.LeftShift(10))

        session[0] shouldBe stack(Material.DIAMOND, 64)
        session[1] shouldBe stack(Material.DIAMOND, 6)
        session[2] shouldBe stack(Material.DIAMOND)
        player.inventory.getItemStack(9).isAir.shouldBeTrue()
        player.inventory.getItemStack(10) shouldBe stack(Material.STONE, 5)
        changes.size shouldBe 2
        session.dispose()
    }

    test("a drag is allowed only over input slots that accept the cursor") {
        val player = fakePlayer()
        val session = inputMenu().open(player)
        player.inventory.cursorItem = stack(Material.DIAMOND, 4)

        player.click(session.inventory, Click.LeftDrag(listOf(0, 1, 20))).isCancelled.shouldBeFalse()
        player.click(session.inventory, Click.LeftDrag(listOf(1, 2))).isCancelled.shouldBeTrue()
        session.dispose()
    }

    test("a double click is cancelled when it would collect an item the menu owns") {
        val player = fakePlayer()
        val session = inputMenu().open(player)

        player.inventory.cursorItem = stack(Material.DIAMOND)
        player.click(player.inventory, Click.Double(9)).isCancelled.shouldBeTrue()
        player.inventory.cursorItem = stack(Material.STONE)
        player.click(player.inventory, Click.Double(9)).isCancelled.shouldBeFalse()
        session.dispose()
    }

    test("items go back to the player who put them in when they leave a shared session") {
        val first = fakePlayer("First")
        val second = fakePlayer("Second")
        val session = inputMenu().create()
        session.open(first)
        session.open(second)
        // A full stack, so the second player's diamonds go to the next slot instead of stacking onto it
        first.inventory.setItemStack(9, stack(Material.DIAMOND, 64))
        second.inventory.setItemStack(9, stack(Material.DIAMOND, 5))
        first.click(first.inventory, Click.LeftShift(9))
        second.click(second.inventory, Click.LeftShift(9))

        first.closeInventory()

        first.inventory.getItemStack(0) shouldBe stack(Material.DIAMOND, 64)
        session[0].isAir.shouldBeTrue()
        session[1] shouldBe stack(Material.DIAMOND, 5)
        session.dispose()
        second.inventory.getItemStack(0) shouldBe stack(Material.DIAMOND, 5)
    }

    test("items in a slot that stops being an input slot go back to their owner") {
        val player = fakePlayer()
        lateinit var hide: () -> Unit
        val session = menu(rows = 1) {
            var visible by state(true)
            hide = { visible = false }
            show({ visible }) {
                slot(0) { policy = SlotPolicy.PLACE }
            }
            // Reads the slot, so it must see the item leave
            text(px(8, 6)) { Component.text(session[0].amount()) }
        }.open(player)
        player.inventory.setItemStack(9, stack(Material.DIAMOND, 2))
        player.click(player.inventory, Click.LeftShift(9))

        hide()
        schedulerManager.processTickEnd()

        player.inventory.getItemStack(0) shouldBe stack(Material.DIAMOND, 2)
        session[0].isAir.shouldBeTrue()
        schedulerManager.processTickEnd()
        session.inventory.title.texts() shouldBe "0"
        session.dispose()
    }

    test("items the menu sets are never given to a player") {
        val player = fakePlayer()
        val session = inputMenu().open(player)
        player.inventory.setItemStack(9, stack(Material.DIAMOND, 2))
        player.click(player.inventory, Click.LeftShift(9))

        session[0] = stack(Material.DIAMOND, 10)
        session.dispose()

        player.inventory.getItemStack(0).isAir.shouldBeTrue()
    }

    test("player inventory clicks reach the hook, which can cancel them") {
        val player = fakePlayer()
        val slots = mutableListOf<Int>()
        val session = menu(rows = 1) {
            onPlayerInventoryClick { click ->
                slots += click.slot
                click.isCancelled = click.slot == 9
            }
        }.open(player)

        player.click(player.inventory, Click.Left(9)).isCancelled.shouldBeTrue()
        player.click(player.inventory, Click.Left(10)).isCancelled.shouldBeFalse()
        slots shouldContainExactly listOf(9, 10)
        session.dispose()
    }

    test("a slot handler can allow a click the policy cancels") {
        val player = fakePlayer()
        val session = menu(rows = 1) {
            slot(0) {
                item { stack(Material.DIAMOND) }
                onClick { it.isCancelled = false }
            }
        }.open(player)

        player.click(session.inventory, Click.Left(0)).isCancelled.shouldBeFalse()
        session.dispose()
    }
})
