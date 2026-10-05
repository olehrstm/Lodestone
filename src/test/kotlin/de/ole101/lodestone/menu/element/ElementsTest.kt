package de.ole101.lodestone.menu.element

import de.ole101.lodestone.menu.async.Resource
import de.ole101.lodestone.menu.async.resource
import de.ole101.lodestone.menu.create
import de.ole101.lodestone.menu.layout.Axis
import de.ole101.lodestone.menu.layout.MenuFonts
import de.ole101.lodestone.menu.layout.SlotArea
import de.ole101.lodestone.menu.menu
import de.ole101.lodestone.menu.open
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fakePlayer
import de.ole101.lodestone.testing.texts
import de.ole101.lodestone.text.glyph.Glyph
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.component.DataComponents
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ElementsTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
        MenuFonts.hoverTooltipStyle = "test:hover"
    }

    afterSpec {
        MenuFonts.hoverTooltipStyle = null
    }

    val amounts = (1..5).toList()
    fun amountsIn(session: de.ole101.lodestone.menu.MenuSession, slots: List<Int>) =
        slots.map { session.inventory.getItemStack(it).amount() }

    test("paginate fills the area page by page and clamps at the ends") {
        lateinit var pager: Pager
        val session = menu(rows = 1) {
            pager = paginate({ amounts }, SlotArea.rect(0..1, 0..0)) { amount ->
                item { ItemStack.of(Material.STONE, amount) }
            }
        }.create()

        pager.pageCount shouldBe 3
        amountsIn(session, listOf(0, 1)) shouldBe listOf(1, 2)

        pager.next()
        pager.next()
        pager.next()
        schedulerManager.processTickEnd()
        pager.page shouldBe 2
        pager.hasNext shouldBe false
        amountsIn(session, listOf(0, 1)) shouldBe listOf(5, 0)
        session.dispose()
    }

    test("scroll moves by whole lines in both axes") {
        lateinit var vertical: Scroller
        lateinit var horizontal: Scroller
        val session = menu(rows = 4) {
            vertical = scroll({ amounts }, SlotArea.rect(0..1, 0..1)) { amount ->
                item { ItemStack.of(Material.STONE, amount) }
            }
            horizontal = scroll({ amounts }, SlotArea.rect(0..1, 2..3), Axis.HORIZONTAL) { amount ->
                item { ItemStack.of(Material.STONE, amount) }
            }
        }.create()

        vertical.max shouldBe 1
        amountsIn(session, listOf(0, 1, 9, 10)) shouldBe listOf(1, 2, 3, 4)
        amountsIn(session, listOf(18, 19, 27, 28)) shouldBe listOf(1, 3, 2, 4)

        vertical.by(5)
        horizontal.to(1)
        schedulerManager.processTickEnd()
        vertical.offset shouldBe 1
        amountsIn(session, listOf(0, 1, 9, 10)) shouldBe listOf(3, 4, 5, 0)
        amountsIn(session, listOf(18, 19, 27, 28)) shouldBe listOf(3, 5, 4, 0)
        session.dispose()
    }

    test("a pressed button ignores clicks until the cooldown ends") {
        val player = fakePlayer()
        val log = mutableListOf<String>()
        val glyph = Glyph(Key.key("test:ui"), 'a', 16)
        val session = menu(rows = 1) {
            button(0, glyph, glyph, cooldown = 1.minutes) {
                onClick { log += "click" }
                onDisabledClick { log += "disabled" }
            }
        }.open(player)

        repeat(2) { EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(0))) }

        log shouldContainExactly listOf("click", "disabled")
        session.dispose()
    }

    test("a cycle slot selects the next option on left click and the previous on right click") {
        val player = fakePlayer()
        lateinit var cycler: Cycler<Material>
        val session = menu(rows = 1) {
            cycler = cycle(0, listOf(Material.STONE, Material.DIRT, Material.SAND)) { ItemStack.of(it) }
        }.open(player)

        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(0)))
        cycler.selected shouldBe Material.DIRT
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Right(0)))
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Right(0)))
        cycler.selected shouldBe Material.SAND

        schedulerManager.processTickEnd()
        session.inventory.getItemStack(0) shouldBe ItemStack.of(Material.SAND)
        session.dispose()
    }

    test("row overlays draw one sprite per row and skip rows without one") {
        val row = Glyph(Key.key("test:ui"), 'r', 176)
        val session = menu(rows = 3) { rowOverlays { if (it == 1) null else row } }.create()

        session.inventory.title.texts() shouldBe "rr"
        session.dispose()
    }

    test("a button spanning two slots is pressed from either slot") {
        val player = fakePlayer()
        val glyph = Glyph(Key.key("test:ui"), 'a', 30, 12)
        var clicks = 0
        val session = menu(rows = 1) {
            button(3..4, 0, glyph, glyph, cooldown = Duration.ZERO) { onClick { clicks++ } }
        }.open(player)

        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(3)))
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(4)))
        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(5)))

        clicks shouldBe 2
        session.dispose()
    }

    test("a hover sprite goes into the button's tooltip, its shadow carrying the target, and hides the tooltip box") {
        val glyph = Glyph(Key.key("test:ui"), 'a', 16, 18)
        val hover = Glyph(Key.key("test:hover"), 'h', 16, 18)
        val session = menu(rows = 1) {
            button(0, glyph, glyph) { hover(hover) }
        }.create()

        val item = session.inventory.getItemStack(0)
        val sprite = item.get(DataComponents.CUSTOM_NAME)!!.children().first().children().first()
        sprite.color() shouldBe TextColor.color(0x4EB000)
        // Centered on slot 0 at x 7 + 9 - 8 and y 17, relative to the center of a 132 pixel high GUI, plus 128.
        // No tooltip text, so width 0 and 1 line.
        sprite.shadowColor() shouldBe ShadowColor.shadowColor(8 - 88 + 128, 17 - 66 + 128, 0, 1)
        item.get(DataComponents.TOOLTIP_STYLE) shouldBe "test:hover"
        session.dispose()
    }

    test("a tab click activates its tab") {
        val player = fakePlayer()
        val glyph = Glyph(Key.key("test:ui"), 'a', 16)
        lateinit var tabs: Tabs<String>
        val session = menu(rows = 1) {
            tabs = tabs("kits") {
                tab("kits", 0, glyph)
                tab("pets", 1, glyph)
            }
        }.open(player)

        EventDispatcher.call(InventoryPreClickEvent(session.inventory, player, Click.Left(1)))
        tabs.active shouldBe "pets"
        session.dispose()
    }

    test("a resource loads on the tick dispatcher and refetches") {
        var calls = 0
        lateinit var resource: Resource<Int>
        val session = menu(rows = 1) {
            resource = resource { ++calls }
        }.create()

        resource.loading shouldBe true
        schedulerManager.processTick()
        resource.loading shouldBe false
        resource.value shouldBe 1

        resource.refetch()
        schedulerManager.processTick()
        resource.value shouldBe 2
        session.dispose()
    }
})
