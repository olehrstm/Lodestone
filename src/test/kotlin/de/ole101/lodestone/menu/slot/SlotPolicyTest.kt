package de.ole101.lodestone.menu.slot

import de.ole101.lodestone.testing.MinestomRegistries
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class SlotPolicyTest : FunSpec({

    beforeSpec { MinestomRegistries.bind() }

    val diamond = ItemStack.of(Material.DIAMOND)
    val stone = ItemStack.of(Material.STONE)
    val air = ItemStack.AIR
    val any: (ItemStack) -> Boolean = { true }

    test("take allows picking up but not putting in or swapping") {
        SlotPolicy.TAKE.allows(Click.Left(0), diamond, air, air, any) shouldBe true
        SlotPolicy.TAKE.allows(Click.LeftShift(0), diamond, air, air, any) shouldBe true
        SlotPolicy.TAKE.allows(Click.Left(0), air, diamond, air, any) shouldBe false
        SlotPolicy.TAKE.allows(Click.HotbarSwap(0, 0), diamond, air, stone, any) shouldBe false
    }

    test("place allows accepted items into empty or matching slots only") {
        val onlyDiamonds: (ItemStack) -> Boolean = { it.material() == Material.DIAMOND }
        SlotPolicy.PLACE.allows(Click.Left(0), air, diamond, air, onlyDiamonds) shouldBe true
        SlotPolicy.PLACE.allows(Click.Right(0), diamond, diamond, air, onlyDiamonds) shouldBe true
        SlotPolicy.PLACE.allows(Click.Left(0), air, stone, air, onlyDiamonds) shouldBe false
        SlotPolicy.PLACE.allows(Click.Left(0), diamond, air, air, onlyDiamonds) shouldBe false
        SlotPolicy.PLACE.allows(Click.Left(0), stone, diamond, air, onlyDiamonds) shouldBe false
    }

    test("free allows swapping, double clicks and drags are always cancelled") {
        SlotPolicy.FREE.allows(Click.Left(0), stone, diamond, air, any) shouldBe true
        SlotPolicy.FREE.allows(Click.HotbarSwap(0, 0), stone, air, diamond, any) shouldBe true
        SlotPolicy.FREE.allows(Click.Double(0), stone, air, air, any) shouldBe false
        SlotPolicy.FREE.allows(Click.LeftDrag(listOf(0, 1)), air, diamond, air, any) shouldBe false
    }
})
