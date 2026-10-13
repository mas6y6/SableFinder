package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.SableFinderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientMessagePacket(Component title,Component message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "message_client"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessagePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ComponentSerialization.STREAM_CODEC,
                    ClientMessagePacket::title,
                    ComponentSerialization.STREAM_CODEC,
                    ClientMessagePacket::message,
                    ClientMessagePacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(@NotNull ClientMessagePacket packet, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), SableFinderClient.SYSTEM_TOAST_ID, packet.title(), packet.message());
        });
    }
}
