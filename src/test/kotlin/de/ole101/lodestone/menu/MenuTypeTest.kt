package de.ole101.lodestone.menu

import de.ole101.lodestone.menu.element.overlay
import de.ole101.lodestone.menu.layout.MenuFonts
import de.ole101.lodestone.menu.layout.px
import de.ole101.lodestone.testing.MinestomRegistries
import de.ole101.lodestone.testing.TestServer
import de.ole101.lodestone.testing.fonts
import de.ole101.lodestone.text.glyph.Glyph
import de.ole101.lodestone.text.glyph.ShiftedFonts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import net.kyori.adventure.key.Key

class MenuTypeTest : FunSpec({

    beforeSpec {
        TestServer.init()
        MinestomRegistries.bind()
        MenuFonts.shiftedFonts = ShiftedFonts { base, y -> Key.key(base.namespace(), "${base.value()}_$y") }
    }

    afterSpec { MenuFonts.shiftedFonts = ShiftedFonts.None }

    test("a glyph with a smaller ascent is moved up by the difference") {
        val cap = Glyph(Key.key("test:ui"), 'a', 16, height = 2, ascent = 2)
        val session = menu(rows = 1) { overlay(cap, px(0, 20)) }.create()

        // 20 - 6 for the title line, - 5 for the ascent
        session.inventory.title.fonts() shouldContain Key.key("test:ui_9")
        session.dispose()
    }
})
