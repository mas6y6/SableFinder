package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SableContraptionResponsePacket(UUID requestId, SableContraptionData contraption) implements ResponsePayload {
    public static final CustomPacketPayload.Type<SableContraptionResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "get_contraption"));

    public static final StreamCodec<ByteBuf, SableContraptionResponsePacket> STREAM_CODEC = StreamCodec.of(SableContraptionResponsePacket::write, SableContraptionResponsePacket::read);

    private static @NotNull SableContraptionResponsePacket read(@NotNull ByteBuf byteBuf) {
        return new SableContraptionResponsePacket(
                UUIDUtil.STREAM_CODEC.decode(byteBuf),
                SableContraptionData.STREAM_CODEC.decode(byteBuf)
        );
    }

    private static void write(@NotNull ByteBuf byteBuf, @NotNull SableContraptionResponsePacket sableContraptionResponsePacket) {
        UUIDUtil.STREAM_CODEC.encode(byteBuf, sableContraptionResponsePacket.requestId());
        SableContraptionData.STREAM_CODEC.encode(byteBuf, sableContraptionResponsePacket.contraption());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(@NotNull SableContraptionResponsePacket packet, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> PacketRequests.complete(packet));
    }
}
