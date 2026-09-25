package de.ole101.lodestone.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponent
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.TooltipDisplay
import net.minestom.server.tag.Tag
import net.minestom.server.utils.Unit as MinestomUnit

public fun item(material: Material, block: ItemBuilder.() -> Unit): ItemStack {
    return ItemBuilder(material).apply(block).build()
}

public fun <B : ItemBuilder> item(builder: B, block: B.() -> Unit): ItemStack {
    return builder.apply(block).build()
}

@DslMarker
public annotation class ItemDsl

@ItemDsl
public open class ItemBuilder(material: Material) {
    protected val builder: ItemStack.Builder = ItemStack.builder(material)

    protected fun Component.noItalic(): Component = decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)

    public fun customName(component: Component) {
        builder.customName(component.noItalic())
    }

    /** Sets an empty custom name, so the item shows no name. */
    public fun noName() {
        builder.customName(Component.empty())
    }

    public fun lore(components: List<Component>) {
        builder.lore(components.map { it.noItalic() })
    }

    public fun lore(vararg components: Component) {
        lore(components.asList())
    }

    public fun amount(amount: Int) {
        builder.amount(amount)
    }

    /** Sets the item model to the resource location [model], for example `"minecraft:diamond"`. */
    public fun itemModel(model: String) {
        builder.itemModel(model)
    }

    public fun <T> data(component: DataComponent<T>, value: T) {
        builder.set(component, value)
    }

    /** Sets the flag data [component], one that has no value, such as [DataComponents.GLIDER]. */
    public fun data(component: DataComponent<MinestomUnit>) {
        builder.set(component)
    }

    /** Hides the whole tooltip, including the name. */
    public fun noTooltip() {
        data(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, emptySet()))
    }

    /** Hides extra tooltip lines such as enchantments and attributes, but keeps the name and lore. */
    public fun hideExtraTooltip() {
        builder.hideExtraTooltip()
    }

    /** Stores [value] under [tag] in the item's custom data. */
    public fun <T> tag(tag: Tag<T>, value: T) {
        builder.setTag(tag, value)
    }

    public open fun build(): ItemStack = builder.build()
}
