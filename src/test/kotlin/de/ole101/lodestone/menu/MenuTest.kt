package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.overlay
import de.ole101.lodestone.menu.element.slot
import de.ole101.lodestone.menu.element.text
import de.ole101.lodestone.menu.layout.VAlign
import de.ole101.lodestone.menu.layout.cell
import de.ole101.lodestone.menu.slot.SlotPolicy
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.*
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.TextAlign
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration.Companion.minutes

private fun stack(material: Material, amount: Int = 1): ItemStack = ItemStack.of(material, amount)

class MenuTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
    }


    test("a state write re-renders the slot at tick end, not before") {
        lateinit var set: (Int) -> Unit
        val session = menu(rows = 1) {
            val amount = state(1)
            set = { amount.value = it }
            slot(0) { item { stack(Material.STONE, amount.value) } }
        }.create()

        set(5)
        session.inventory.getItemStack(0).amount() shouldBe 1
        schedulerManager.processTickEnd()
        session.inventory.getItemStack(0).amount() shouldBe 5
        session.dispose()
    }

    test("show mounts and unmounts its elements and effects") {
        val log = mutableListOf<String>()
        lateinit var toggle: () -> Unit
        val session = menu(rows = 1) {
            var visible by state(true)
            toggle = { visible = !visible }
            show({ visible }) {
                slot(0) { item { stack(Material.STONE) } }
                effect { onCleanup { log += "cleanup" } }
            }
        }.create()

        session.inventory.getItemStack(0) shouldBe stack(Material.STONE)
        toggle()
        schedulerManager.processTickEnd()
        session.inventory.getItemStack(0).isAir.shouldBeTrue()
        log shouldContainExactly listOf("cleanup")
        session.dispose()
    }

    test("each keeps blocks of items that stay and follows the list order") {
        val mounts = mutableListOf<String>()
        lateinit var set: (List<String>) -> Unit
        val session = menu(rows = 1) {
            var names by state(listOf("a", "b"))
            set = { names = it }
            each({ names }) { name ->
                mounts += name
                text(cell(0, 0)) { Component.text(name) }
            }
        }.create()

        set(listOf("b", "c"))
        schedulerManager.processTickEnd()

        mounts shouldContainExactly listOf("a", "b", "c")
        session.inventory.title.texts() shouldBe "bc"
        session.dispose()
    }

    test("the title draws on top of other text") {
        val session = menu(rows = 1) {
            title(TextAlign.CENTER) { Component.text("Shop") }
            text(cell(0, 0)) { Component.text("x") }
        }.create()

        session.inventory.title.texts() shouldBe "xShop"
        session.dispose()
    }

    test("a glyph with a height is centered vertically in its cell") {
        val glyph = Glyph(Key.key("test:ui"), 'a', 16, 8)
        val session = menu(rows = 1) { overlay(glyph, cell(0, 0), vAlign = VAlign.MIDDLE) }.create()

        // Slot top 17 + (18 - 8) / 2 = 22, minus the title line at 6
        session.inventory.title.fonts() shouldContain Key.key("test:ui")
        session.inventory.title.shadows() shouldContain ShadowColor.shadowColor(0x4EB810FF)
        session.inventory.title.colors() shouldContain TextColor.color(0x4EB000)
        session.dispose()
    }

    test("aligning a glyph without a height is rejected") {
        shouldThrow<IllegalArgumentException> {
            menu(rows = 1) { overlay(Glyph(Key.key("test:ui"), 'a', 16), cell(0, 0), vAlign = VAlign.BOTTOM) }.create()
        }
    }

    test("open and close run hooks and dispose an empty session") {
        val log = mutableListOf<String>()
        val player = fakePlayer()
        val session = menu(rows = 1) {
            onOpen { log += "open ${it.username}" }
            onClose { viewer, reason -> log += "close ${viewer.username} $reason" }
        }.open(player)

        session.viewers shouldBe setOf(player)
        player.closeInventory()

        log shouldContainExactly listOf("open Tester", "close Tester SERVER")
        session.viewers.shouldBeEmpty()
        session.isDisposed.shouldBeTrue()
    }

    test("a manual session survives being closed and can be reopened") {
        val player = fakePlayer()
        val session = menu(rows = 1) {}.create(DisposePolicy.MANUAL)

        session.open(player)
        session.close()
        session.isDisposed.shouldBeFalse()
        session.open(player)
        session.viewers shouldBe setOf(player)
        session.dispose()
        shouldThrow<IllegalStateException> { session.open(player) }
    }

    test("a viewer that opens another inventory is removed at the next tick") {
        val reasons = mutableListOf<CloseReason>()
        val player = fakePlayer()
        menu(rows = 1) { onClose { _, reason -> reasons += reason } }.open(player)
        val other = menu(rows = 1) {}.open(player)

        schedulerManager.processTick()
        reasons shouldContainExactly listOf(CloseReason.REPLACED)
        other.dispose()
    }

    test("clicks are cancelled by default and reach the slot handler") {
        val player = fakePlayer()
        val clicked = mutableListOf<Int>()
        val session = menu(rows = 1) {
            slot(2) { onClick { clicked += it.slot } }
        }.open(player)

        val event = InventoryPreClickEvent(session.inventory, player, Click.Left(2))
        EventDispatcher.call(event)

        event.isCancelled.shouldBeTrue()
        clicked shouldContainExactly listOf(2)
        session.dispose()
    }

    test("clicks within the click cooldown are cancelled and reach no handler") {
        val player = fakePlayer()
        var clicks = 0
        val session = menu(rows = 1) {
            clickCooldown(1.minutes)
            slot(0) { onClick { clicks++ } }
        }.open(player)

        repeat(3) { EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(0))) }

        clicks shouldBe 1
        session.dispose()
    }

    test("an input slot accepts placing, reports player changes and returns items on dispose") {
        val player = fakePlayer()
        val changes = mutableListOf<ItemStack>()
        val session = menu(rows = 1) {
            slot(0) {
                policy = SlotPolicy.PLACE
                accepts { it.material() == Material.DIAMOND }
                onChange { _, new -> changes += new }
            }
        }.open(player)
        player.inventory.cursorItem = stack(Material.DIAMOND)

        val event = InventoryPreClickEvent(session.inventory, player, Click.Left(0))
        EventDispatcher.call(event)
        event.isCancelled.shouldBeFalse()

        session.inventory.setItemStack(0, stack(Material.DIAMOND))
        changes shouldContainExactly listOf(stack(Material.DIAMOND))

        player.inventory.cursorItem = ItemStack.AIR
        session.dispose()
        session.inventory.getItemStack(0).isAir.shouldBeTrue()
        player.inventory.getItemStack(0) shouldBe stack(Material.DIAMOND)
    }

    test("setup failures reach the caller") {
        shouldThrow<IllegalArgumentException> {
            menu(rows = 1) { slot(9) {} }.create()
        }
        shouldThrow<IllegalArgumentException> { menu(rows = 7) {} }
    }
})
