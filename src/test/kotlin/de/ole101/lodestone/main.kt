package de.ole101.lodestone

import de.ole101.lodestone.event.listen
import de.ole101.lodestone.sign.signInput
import de.ole101.lodestone.text.mini
import de.ole101.lodestone.text.send
import de.ole101.lodestone.text.sendMini
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block

fun main() {
    val server = MinecraftServer.init(Auth.Online())

    val instanceContainer = instanceManager.createInstanceContainer()
    instanceContainer.setChunkSupplier(::LightingChunk)
    instanceContainer.setGenerator { unit -> unit.modifier().fillHeight(1, 40, Block.GRASS_BLOCK) }

//    MinecraftServer.getCommandManager().register(TestCommand())

    listen<AsyncPlayerConfigurationEvent> { event ->
        val player = event.player

        event.spawningInstance = instanceContainer
        player.respawnPoint = Pos(0.0, 42.0, 0.0)
    }
    listen<PlayerSpawnEvent> { event ->
        val player = event.player

        val message = "test123 <!red>".mini()
        message.send(player)
        "<small>Hello 123!</small>".sendMini(player)

        val input = signInput {
            onInput { player, input -> player.sendMessage(input) }
        }
        input.open(player)
    }

    server.start("0.0.0.0", 25565)
}
