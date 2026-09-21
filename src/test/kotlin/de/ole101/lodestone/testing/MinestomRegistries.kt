package de.ole101.lodestone.testing

import net.minestom.server.registry.Registries

/**
 * Minestom's static material registry only gets its component prototypes bound when [Registries] is built, which
 * normally happens while the server boots. Anything that resolves item components needs this first, and building the
 * registries once is enough since the binding it performs is global.
 */
internal object MinestomRegistries {

    private val vanilla by lazy { Registries.vanilla() }

    fun bind() {
        this.vanilla
    }
}
