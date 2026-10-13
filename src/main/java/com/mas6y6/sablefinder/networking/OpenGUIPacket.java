package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.client.ClientPayloadHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import java.util.UUID;

public record OpenGUIPacket(Optional<UUID> selectedContraptionID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenGUIPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "open_gui"));

    public static final StreamCodec<ByteBuf, OpenGUIPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
                OpenGUIPacket::selectedContraptionID,
                OpenGUIPacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(@NotNull OpenGUIPacket customPacketPayload, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            ClientPayloadHandler.openGUI(customPacketPayload.selectedContraptionID);
        });
    }
}