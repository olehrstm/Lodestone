package de.ole101.lodestone.testing

import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.UUID

private class FakeConnection : PlayerConnection() {
    override fun sendPacket(packet: SendablePacket) {}

    override fun getRemoteAddress(): SocketAddress = InetSocketAddress(0)
}

/** A player without a client, enough to open inventories and receive items. Needs [TestServer.init]. */
internal fun fakePlayer(name: String = "Tester"): Player = Player(FakeConnection(), GameProfile(UUID.randomUUID(), name))
