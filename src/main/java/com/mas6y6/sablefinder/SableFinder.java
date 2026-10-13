package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.config.ConfigServer;
import com.mas6y6.sablefinder.networking.Networking;
import com.mas6y6.sablefinder.networking.PacketRequests;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

import java.io.IOException;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SableFinder.MODID)
public class SableFinder {
    public static final String MODID = "sablefinder";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static @Nullable MinecraftServer server;

    // Resolved from the loaded mod metadata, so it always matches the jar's mod_version.
    private static String version = "unknown";

    public SableFinder(@NotNull IEventBus modEventBus, @NotNull ModContainer modContainer) {
        version = modContainer.getModInfo().getVersion().toString();
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigServer.SPEC);
        modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onCommandRegister);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStop);
    }

    @SubscribeEvent
    public void onPacketRegister(@NotNull RegisterPayloadHandlersEvent event) {
        var payloadregister = event.registrar("1").optional();
        Networking.registerPayloadHandlers(payloadregister);
    }

    private void onCommandRegister(@NotNull RegisterCommandsEvent event) {
        SFCommands.register(event.getDispatcher());
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PacketRequests.failAll(new IOException("Player disconnected"));
    }

    public static @Nullable MinecraftServer getServer() {
        return server;
    }

    /**
     * The sableFinder version of the side this JVM is running. Both sides compare this
     * string during the handshake to make sure they are running the same build.
     */
    public static @NotNull String getVersion() {
        return version;
    }

    private void onServerStart(@NotNull ServerStartedEvent event) {
        server = event.getServer();
    }

    private void onServerStop(ServerStoppedEvent event) {
        server = null;
    }
}
