package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.SableFinderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientToastMessagePacket(int slot, Component title, Component message) implements CustomPacketPayload {
    public static final int SLOT_SYSTEM = 0;
    public static final int SLOT_PROGRESS = 1;

    public static final CustomPacketPayload.Type<ClientToastMessagePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "message_client"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientToastMessagePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ClientToastMessagePacket::slot,
                    ComponentSerialization.STREAM_CODEC,
                    ClientToastMessagePacket::title,
                    ComponentSerialization.STREAM_CODEC,
                    ClientToastMessagePacket::message,
                    ClientToastMessagePacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(@NotNull ClientToastMessagePacket packet, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            final SystemToastId id = packet.slot() == SLOT_PROGRESS
                    ? SableFinderClient.PROGRESS_TOAST_ID
                    : SableFinderClient.SYSTEM_TOAST_ID;

            SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), id, packet.title(), packet.message());
        });
    }
}
