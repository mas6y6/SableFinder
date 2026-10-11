package com.mas6y6.sablefinder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.UUID;


public class ClientPayloadHandler {
    public static void openGUI(CompoundTag sableContraptions, Optional<UUID> selectedContraptionID) {
        Minecraft.getInstance().setScreen(new SableFinderScreen(sableContraptions, selectedContraptionID));
    }
}
