package de.ole101.lodestone.item

import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.plain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.TooltipDisplay
import net.minestom.server.tag.Tag

class ItemBuilderTest : FunSpec({

    beforeSpec { MinestomRegistries.bind() }

    fun ItemStack.customName(): Component? = get(DataComponents.CUSTOM_NAME)

    fun ItemStack.lore(): List<Component>? = get(DataComponents.LORE)

    context("item(material)") {
        test("builds a single item of that material") {
            val item = item(Material.DIAMOND_SWORD) { }

            item.material() shouldBe Material.DIAMOND_SWORD
            item.amount() shouldBe 1
        }

        test("sets no components when the block is empty") {
            item(Material.STONE) { }.componentPatch().isEmpty() shouldBe true
        }
    }

    context("customName") {
        test("sets the name") {
            val item = item(Material.STONE) { customName(Component.text("Rock")) }

            item.customName()?.plain() shouldBe "Rock"
        }

        test("disables italics, which Minecraft would otherwise add") {
            val item = item(Material.STONE) { customName(Component.text("Rock")) }

            item.customName()?.decoration(TextDecoration.ITALIC) shouldBe TextDecoration.State.FALSE
        }

        test("keeps italics that were asked for explicitly") {
            val name = Component.text("Rock").decorate(TextDecoration.ITALIC)

            val item = item(Material.STONE) { customName(name) }

            item.customName()?.decoration(TextDecoration.ITALIC) shouldBe TextDecoration.State.TRUE
        }

        test("keeps the rest of the style") {
            val name = Component.text("Rock").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)

            val item = item(Material.STONE) { customName(name) }

            item.customName()?.color() shouldBe NamedTextColor.GOLD
            item.customName()?.decoration(TextDecoration.BOLD) shouldBe TextDecoration.State.TRUE
        }

        test("the last call wins") {
            val item = item(Material.STONE) {
                customName(Component.text("First"))
                customName(Component.text("Second"))
            }

            item.customName()?.plain() shouldBe "Second"
        }
    }

    context("noName") {
        test("sets an empty name so Minecraft shows nothing") {
            item(Material.STONE) { noName() }.customName() shouldBe Component.empty()
        }

        test("overrides a name set earlier") {
            val item = item(Material.STONE) {
                customName(Component.text("Rock"))
                noName()
            }

            item.customName() shouldBe Component.empty()
        }
    }

    context("lore") {
        test("sets the lines from a list") {
            val item = item(Material.STONE) { lore(listOf(Component.text("one"), Component.text("two"))) }

            item.lore()?.map { it.plain() } shouldBe listOf("one", "two")
        }

        test("sets the lines from varargs") {
            val item = item(Material.STONE) { lore(Component.text("one"), Component.text("two")) }

            item.lore()?.map { it.plain() } shouldBe listOf("one", "two")
        }

        test("disables italics on every line") {
            val item = item(Material.STONE) { lore(Component.text("one"), Component.text("two")) }

            item.lore()?.forEach {
                it.decoration(TextDecoration.ITALIC) shouldBe TextDecoration.State.FALSE
            }
        }

        test("keeps italics that were asked for explicitly") {
            val line = Component.text("italic").decorate(TextDecoration.ITALIC)

            val item = item(Material.STONE) { lore(line) }

            item.lore()?.single()?.decoration(TextDecoration.ITALIC) shouldBe TextDecoration.State.TRUE
        }

        test("accepts an empty lore") {
            item(Material.STONE) { lore(emptyList()) }.lore() shouldBe emptyList()
        }
    }

    context("amount") {
        test("sets the stack size") {
            item(Material.STONE) { amount(16) }.amount() shouldBe 16
        }
    }

    context("itemModel") {
        test("sets the model") {
            item(Material.STONE) { itemModel("lodestone:rock") }
                .get(DataComponents.ITEM_MODEL) shouldBe "lodestone:rock"
        }
    }

    context("data") {
        test("sets a component with a value") {
            item(Material.STONE) { data(DataComponents.MAX_STACK_SIZE, 8) }
                .get(DataComponents.MAX_STACK_SIZE) shouldBe 8
        }

        test("sets a marker component without a value") {
            item(Material.STONE) { data(DataComponents.GLIDER) }
                .componentPatch().has(DataComponents.GLIDER) shouldBe true
        }

    }

    context("noTooltip") {
        test("hides the tooltip entirely") {
            item(Material.STONE) { noTooltip() }
                .get(DataComponents.TOOLTIP_DISPLAY) shouldBe TooltipDisplay(true, emptySet())
        }
    }

    context("hideExtraTooltip") {
        test("keeps the tooltip but hides the extra lines") {
            val display = item(Material.STONE) { hideExtraTooltip() }.get(DataComponents.TOOLTIP_DISPLAY)

            display?.hideTooltip() shouldBe false
            display?.hiddenComponents()?.shouldContain(DataComponents.ATTRIBUTE_MODIFIERS)
        }
    }

    context("tag") {
        test("stores a tag value") {
            val tag = Tag.String("owner")

            item(Material.STONE) { tag(tag, "ole") }.getTag(tag) shouldBe "ole"
        }

    }

    context("item(builder)") {
        class SwordBuilder : ItemBuilder(Material.DIAMOND_SWORD) {
            fun sharpness(level: Int) {
                customName(Component.text("Sword $level"))
            }

            override fun build(): ItemStack {
                noTooltip()
                return super.build()
            }
        }

        test("runs the block against the given builder") {
            val item = item(SwordBuilder()) { sharpness(5) }

            item.material() shouldBe Material.DIAMOND_SWORD
            item.customName()?.plain() shouldBe "Sword 5"
        }

        test("uses the builder's own build override") {
            val item = item(SwordBuilder()) { sharpness(5) }

            item.get(DataComponents.TOOLTIP_DISPLAY) shouldBe TooltipDisplay(true, emptySet())
        }
    }
})
