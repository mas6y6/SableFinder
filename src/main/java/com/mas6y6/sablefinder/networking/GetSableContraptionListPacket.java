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

public record GetSableContraptionListPacket(UUID requestId, CompoundTag sableContraptions) implements ResponsePayload {
    public static final CustomPacketPayload.Type<GetSableContraptionListPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "get_contraption_list"));

    public static final StreamCodec<ByteBuf, GetSableContraptionListPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    GetSableContraptionListPacket::requestId,
                    ByteBufCodecs.COMPOUND_TAG,
                    GetSableContraptionListPacket::sableContraptions,
                    GetSableContraptionListPacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(@NotNull GetSableContraptionListPacket packet, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> PacketRequests.complete(packet));
    }
}
