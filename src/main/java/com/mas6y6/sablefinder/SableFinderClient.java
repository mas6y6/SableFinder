package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.client.ClientPayloadHandler;
import com.mas6y6.sablefinder.client.SableFinderScreen;
import com.mas6y6.sablefinder.config.ConfigClient;
import com.mas6y6.sablefinder.networking.PacketRequests;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;

@Mod(value = SableFinder.MODID, dist = Dist.CLIENT)
public class SableFinderClient {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final SystemToast.SystemToastId SYSTEM_TOAST_ID = new SystemToast.SystemToastId();

    public SableFinderClient(@NotNull IEventBus modEventBus, @NotNull ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC);
        modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    @SubscribeEvent
    public void registerKeybinds(@NotNull RegisterKeyMappingsEvent event) {
        event.register(Keybinds.REFRESH_KEY.get());
        event.register(Keybinds.GO_TO_NEXT_CONTRAPTION_KEY.get());
        event.register(Keybinds.GO_TO_PREVIOUS_CONTRAPTION_KEY.get());
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PacketRequests.failAll(new IOException("Client disconnected"));
        ClientPayloadHandler.setServerVersion(null);
        // The remembered contraption belongs to the world we are leaving.
        SableFinderScreen.forgetSession();
    }
}