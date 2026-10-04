package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.slot
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fakePlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class NavigationTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
    }

    test("a parent survives while the player is in a child and keeps its state for going back") {
        val player = fakePlayer()
        val reasons = mutableListOf<CloseReason>()
        lateinit var set: (Int) -> Unit
        val parent = menu(rows = 1) {
            var amount by state(1)
            set = { amount = it }
            slot(0) { item { ItemStack.of(Material.STONE, amount) } }
            onClose { _, reason -> reasons += reason }
        }.open(player)
        set(7)
        schedulerManager.processTickEnd()

        val child = menu(rows = 1) {}.open(player, back = parent)
        parent.isDisposed.shouldBeFalse()
        parent.viewers.isEmpty().shouldBeTrue()
        child.canGoBack(player).shouldBeTrue()

        child.goBack(player).shouldBeTrue()
        parent.viewers shouldBe setOf(player)
        parent[0].amount() shouldBe 7
        child.isDisposed.shouldBeTrue()
        reasons shouldContainExactly listOf(CloseReason.NAVIGATED)
        parent.dispose()
    }

    test("closing a child releases the whole way back") {
        val player = fakePlayer()
        val first = menu(rows = 1) {}.open(player)
        val second = menu(rows = 1) {}.open(player, back = first)
        val third = menu(rows = 1) {}.open(player, back = second)

        player.closeInventory()

        third.isDisposed.shouldBeTrue()
        second.isDisposed.shouldBeTrue()
        first.isDisposed.shouldBeTrue()
    }

    test("clicks outside the GUI reach the outside click hook") {
        val player = fakePlayer()
        var outside = 0
        val session = menu(rows = 1) { onOutsideClick { outside++ } }.open(player)

        EventDispatcher.call(InventoryPreClickEvent(player.inventory, player, Click.LeftDropCursor()))
        EventDispatcher.call(InventoryPreClickEvent(player.inventory, player, Click.Left(9)))

        outside shouldBe 1
        session.dispose()
    }

    test("going back from a session without a way back does nothing") {
        val player = fakePlayer()
        val session = menu(rows = 1) {}.open(player)

        session.goBack(player).shouldBeFalse()
        session.viewers shouldBe setOf(player)
        session.dispose()
    }
})
