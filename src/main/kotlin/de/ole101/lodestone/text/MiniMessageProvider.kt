package de.ole101.lodestone.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

public val LodestoneTags: TagResolver = TagResolver.resolver(ColorTag(), SmallCapsTag.RESOLVER)

public object MiniMessageProvider {

    @Volatile
    public var instance: MiniMessage = build(TagResolver.empty())
        private set

    public fun configure(extra: TagResolver) {
        this.instance = build(extra)
    }

    public fun parse(message: String): Component {
        return this.instance.deserialize(message)
    }

    private fun build(extra: TagResolver): MiniMessage {
        return MiniMessage.builder()
            .tags(TagResolver.resolver(TagResolver.standard(), LodestoneTags, extra))
            .build()
    }
}
