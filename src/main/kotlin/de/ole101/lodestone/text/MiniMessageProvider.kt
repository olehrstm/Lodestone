package de.ole101.lodestone.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * MiniMessage tags added by Lodestone.
 *
 * - `<!name>` colors text with a color from [Colors], for example `<!aliceblue>`.
 * - `<small>` turns letters into small caps, for example `<small>hello</small>` gives `ʜᴇʟʟᴏ`.
 *
 * [MiniMessageProvider] always includes them.
 */
public val LodestoneTags: TagResolver = TagResolver.resolver(ColorTag(), SmallCapsTag.RESOLVER)

/**
 * Shared [MiniMessage] instance used by [mini] and the other text helpers.
 *
 * It always supports the standard tags and [LodestoneTags]. Call [configure] to add more.
 */
public object MiniMessageProvider {

    @Volatile
    public var instance: MiniMessage = build(TagResolver.empty())
        private set

    /**
     * Rebuilds [instance] with the standard tags, [LodestoneTags] and [extra].
     *
     * Each call replaces the extra tags of the previous call. To keep several, pass them together
     * with `TagResolver.resolver(...)`.
     */
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
