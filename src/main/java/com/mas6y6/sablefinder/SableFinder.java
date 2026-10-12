package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.config.ConfigServer;
import com.mas6y6.sablefinder.networking.Networking;
import com.mas6y6.sablefinder.networking.PacketRequests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
    private static MinecraftServer server;

    public SableFinder(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigServer.SPEC);
        modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onCommandRegister);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStop);
    }

    @SubscribeEvent
    public void onPacketRegister(RegisterPayloadHandlersEvent event) {
        var payloadregister = event.registrar("1").optional();
        Networking.registerPayloadHandlers(payloadregister);
    }

    private void onCommandRegister(RegisterCommandsEvent event) {
        SFCommands.register(event.getDispatcher());
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PacketRequests.failAll(new IOException("Player disconnected"));
    }

    public static @Nullable MinecraftServer getServer() {
        return server;
    }

    private void onServerStart(ServerStartedEvent event) {
        server = event.getServer();
    }

    private void onServerStop(ServerStoppedEvent event) {
        server = null;
    }
}
