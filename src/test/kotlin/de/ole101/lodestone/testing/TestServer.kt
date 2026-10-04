package de.ole101.lodestone.testing

import net.minestom.server.MinecraftServer

/**
 * Initializes one Minestom server process for all specs. Every `MinecraftServer.init()` replaces the process, but the
 * `Minestom` dispatcher stays bound to the scheduler of the first one, so specs must not create a second.
 */
internal object TestServer {

    private val server by lazy { MinecraftServer.init() }

    fun init() {
        this.server
    }
}
