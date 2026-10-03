package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.config.ConfigServer;
import com.mas6y6.sablefinder.networking.Networking;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SableFinder.MODID)
public class SableFinder {
    public static final String MODID = "sablefinder";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SableFinder(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigServer.SPEC);
        modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onCommandRegister);
    }

    @SubscribeEvent
    public void onPacketRegister(RegisterPayloadHandlersEvent event) {
        var payloadregister = event.registrar("1").optional();
        Networking.registerPayloadHandlers(payloadregister);


    }

    private void onCommandRegister(RegisterCommandsEvent event) {
        SFCommands.register(event.getDispatcher());
    }
}
