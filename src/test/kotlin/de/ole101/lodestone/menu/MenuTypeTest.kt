package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.overlay
import de.ole101.lodestone.menu.layout.px
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fonts
import de.ole101.lodestone.testing.shadows
import de.ole101.lodestone.text.glyph.Glyph
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.ShadowColor

class MenuTypeTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
    }


    test("a glyph with a smaller ascent is moved up by the difference") {
        val cap = Glyph(Key.key("test:ui"), 'a', 16, height = 2, ascent = 2)
        val session = menu(rows = 1) { overlay(cap, px(0, 20)) }.create()

        // 20 - 6 for the title line, - 5 for the ascent
        session.inventory.title.fonts() shouldContain Key.key("test:ui")
        session.inventory.title.shadows() shouldContain ShadowColor.shadowColor(0x4EB809FF)
        session.dispose()
    }
})
