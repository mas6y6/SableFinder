package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SearchForContraptionsResponsePacket(UUID requestId, CompoundTag contraptions) implements ResponsePayload {
    public static final CustomPacketPayload.Type<SearchForContraptionsResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "search_contraptions_response"));

    public static final StreamCodec<ByteBuf, SearchForContraptionsResponsePacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    SearchForContraptionsResponsePacket::requestId,
                    ByteBufCodecs.COMPOUND_TAG,
                    SearchForContraptionsResponsePacket::contraptions,
                    SearchForContraptionsResponsePacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient(IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> PacketRequests.complete(this));
    }
}