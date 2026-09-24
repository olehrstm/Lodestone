package de.ole101.lodestone

import net.minestom.server.MinecraftServer
import net.minestom.server.advancements.AdvancementManager
import net.minestom.server.adventure.ClickCallbackManager
import net.minestom.server.adventure.bossbar.BossBarManager
import net.minestom.server.command.CommandManager
import net.minestom.server.dialog.Dialog
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.animal.*
import net.minestom.server.entity.metadata.animal.tameable.CatVariant
import net.minestom.server.entity.metadata.animal.tameable.WolfSoundVariant
import net.minestom.server.entity.metadata.animal.tameable.WolfVariant
import net.minestom.server.entity.metadata.cube.SulfurCubeArchetype
import net.minestom.server.entity.metadata.other.PaintingVariant
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.exception.ExceptionManager
import net.minestom.server.instance.InstanceManager
import net.minestom.server.instance.block.BlockManager
import net.minestom.server.instance.block.banner.BannerPattern
import net.minestom.server.instance.block.jukebox.JukeboxSong
import net.minestom.server.item.armor.TrimMaterial
import net.minestom.server.item.armor.TrimPattern
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.item.instrument.Instrument
import net.minestom.server.listener.manager.PacketListenerManager
import net.minestom.server.message.ChatType
import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.packet.PacketParser
import net.minestom.server.recipe.RecipeManager
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.scoreboard.TeamManager
import net.minestom.server.timer.SchedulerManager
import net.minestom.server.world.DimensionType
import net.minestom.server.world.biome.Biome
import net.minestom.server.world.clock.WorldClock
import net.minestom.server.world.timeline.Timeline

public inline val globalEventHandler: GlobalEventHandler
    get() = MinecraftServer.getGlobalEventHandler()

public inline val packetListenerManager: PacketListenerManager
    get() = MinecraftServer.getPacketListenerManager()

public inline val instanceManager: InstanceManager
    get() = MinecraftServer.getInstanceManager()

public inline val blockManager: BlockManager
    get() = MinecraftServer.getBlockManager()

public inline val commandManager: CommandManager
    get() = MinecraftServer.getCommandManager()

public inline val recipeManager: RecipeManager
    get() = MinecraftServer.getRecipeManager()

public inline val teamManager: TeamManager
    get() = MinecraftServer.getTeamManager()

public inline val schedulerManager: SchedulerManager
    get() = MinecraftServer.getSchedulerManager()

public inline val exceptionManager: ExceptionManager
    get() = MinecraftServer.getExceptionManager()

public inline val connectionManager: ConnectionManager
    get() = MinecraftServer.getConnectionManager()

public inline val bossBarManager: BossBarManager
    get() = MinecraftServer.getBossBarManager()

public inline val packetParser: PacketParser.Client
    get() = MinecraftServer.getPacketParser()

public inline val advancementManager: AdvancementManager
    get() = MinecraftServer.getAdvancementManager()

public inline val clickCallbackManager: ClickCallbackManager
    get() = MinecraftServer.getClickCallbackManager()

public inline val chatTypeRegistry: DynamicRegistry<ChatType>
    get() = MinecraftServer.getChatTypeRegistry()

public inline val dialogRegistry: DynamicRegistry<Dialog>
    get() = MinecraftServer.getDialogRegistry()

public inline val dimensionTypeRegistry: DynamicRegistry<DimensionType>
    get() = MinecraftServer.getDimensionTypeRegistry()

public inline val biomeRegistry: DynamicRegistry<Biome>
    get() = MinecraftServer.getBiomeRegistry()

public inline val damageTypeRegistry: DynamicRegistry<DamageType>
    get() = MinecraftServer.getDamageTypeRegistry()

public inline val trimMaterialRegistry: DynamicRegistry<TrimMaterial>
    get() = MinecraftServer.getTrimMaterialRegistry()

public inline val trimPatternRegistry: DynamicRegistry<TrimPattern>
    get() = MinecraftServer.getTrimPatternRegistry()

public inline val bannerPatternRegistry: DynamicRegistry<BannerPattern>
    get() = MinecraftServer.getBannerPatternRegistry()

public inline val wolfVariantRegistry: DynamicRegistry<WolfVariant>
    get() = MinecraftServer.getWolfVariantRegistry()

public inline val wolfSoundVariantRegistry: DynamicRegistry<WolfSoundVariant>
    get() = MinecraftServer.getWolfSoundVariantRegistry()

public inline val catVariantRegistry: DynamicRegistry<CatVariant>
    get() = MinecraftServer.getCatVariantRegistry()

public inline val chickenVariantRegistry: DynamicRegistry<ChickenVariant>
    get() = MinecraftServer.getChickenVariantRegistry()

public inline val cowVariantRegistry: DynamicRegistry<CowVariant>
    get() = MinecraftServer.getCowVariantRegistry()

public inline val frogVariantRegistry: DynamicRegistry<FrogVariant>
    get() = MinecraftServer.getFrogVariantRegistry()

public inline val pigVariantRegistry: DynamicRegistry<PigVariant>
    get() = MinecraftServer.getPigVariantRegistry()

public inline val zombieNautilusVariantRegistry: DynamicRegistry<ZombieNautilusVariant>
    get() = MinecraftServer.getZombieNautilusVariantRegistry()

public inline val enchantmentRegistry: DynamicRegistry<Enchantment>
    get() = MinecraftServer.getEnchantmentRegistry()

public inline val paintingVariantRegistry: DynamicRegistry<PaintingVariant>
    get() = MinecraftServer.getPaintingVariantRegistry()

public inline val jukeboxSongRegistry: DynamicRegistry<JukeboxSong>
    get() = MinecraftServer.getJukeboxSongRegistry()

public inline val instrumentRegistry: DynamicRegistry<Instrument>
    get() = MinecraftServer.getInstrumentRegistry()

public inline val timelineRegistry: DynamicRegistry<Timeline>
    get() = MinecraftServer.getTimelineRegistry()

public inline val worldClockRegistry: DynamicRegistry<WorldClock>
    get() = MinecraftServer.getWorldClockRegistry()

public inline val sulfurCubeArchetypeRegistry: DynamicRegistry<SulfurCubeArchetype>
    get() = MinecraftServer.getSulfurCubeArchetypeRegistry()
