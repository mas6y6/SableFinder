package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.config.ConfigClient;
import com.mas6y6.sablefinder.config.ConfigServer;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(value = SableFinder.MODID, dist = Dist.CLIENT)
public class SableFinderClient {
    public static final Logger LOGGER = LogUtils.getLogger();

    public SableFinderClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC);
    }
}
