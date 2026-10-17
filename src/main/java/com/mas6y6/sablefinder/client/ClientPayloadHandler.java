package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.SableFinderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


public class ClientPayloadHandler {
    private static @Nullable String serverVersion;

    public static @Nullable String serverVersion() {
        return serverVersion;
    }

    public static void openGUI(@NotNull Optional<UUID> selectedContraptionID) {
        if (!isCompatible()) {
            warnVersionMismatch();
            return;
        }

        Minecraft.getInstance().setScreen(new SableFinderScreen(selectedContraptionID));
    }

    public static void setServerVersion(@Nullable String version) {
        final boolean changed = !Objects.equals(serverVersion, version);
        serverVersion = version;

        if (changed && version != null && !version.equals(SableFinder.getVersion())) {
            warnVersionMismatch();
        }
    }

    public static boolean isCompatible() {
        return serverVersion != null && serverVersion.equals(SableFinder.getVersion());
    }

    public static boolean allowOperation() {
        if (isCompatible()) {
            return true;
        }

        warnVersionMismatch();
        return false;
    }

    private static void warnVersionMismatch() {
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
    }
}