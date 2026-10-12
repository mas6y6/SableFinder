package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.search.SearchEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public record CancelSearchPacket(UUID requestId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CancelSearchPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "cancel_search"));

    public static final StreamCodec<ByteBuf, CancelSearchPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CancelSearchPacket::requestId,
            CancelSearchPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(CancelSearchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> SearchEngine.cancel(packet.requestId()));
    }
}
