package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.SableFinderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;


public class ClientPayloadHandler {
    private static @Nullable String serverVersion;

    public static void openGUI(CompoundTag sableContraptions, @NotNull Optional<UUID> selectedContraptionID) {
        if (!isServerVersionCompatible()) {
            SystemToast.addOrUpdate(
                    Minecraft.getInstance().getToasts(),
                    SableFinderClient.SYSTEM_TOAST_ID,
                    Component.translatable("sablefinder.message.version_mismatch.title"),
                    Component.translatable(
                            "sablefinder.message.version_mismatch.body",
                            SableFinder.getVersion(),
                            serverVersion == null ? "unknown" : serverVersion
                    )
            );
            return;
        }

        Minecraft.getInstance().setScreen(new SableFinderScreen(sableContraptions, selectedContraptionID));
    }

    public static void setServerVersion(@Nullable String version) {
        serverVersion = version;
    }

    private static boolean isServerVersionCompatible() {
        return serverVersion == null || serverVersion.equals(SableFinder.getVersion());
    }
}