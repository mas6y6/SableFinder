package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.client.SableContraptionClient;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GetSableContraptionPacket(SableContraptionData contraption) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GetSableContraptionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "get_contraption"));

    public static final StreamCodec<ByteBuf, GetSableContraptionPacket> STREAM_CODEC = StreamCodec.of(GetSableContraptionPacket::write, GetSableContraptionPacket::read);

    private static GetSableContraptionPacket read(ByteBuf byteBuf) {
        return new GetSableContraptionPacket(
            SableContraptionData.STREAM_CODEC.decode(byteBuf)
        );
    }

    private static void write(ByteBuf byteBuf, GetSableContraptionPacket getSableContraptionPacket) {
        SableContraptionData.STREAM_CODEC.encode(byteBuf, getSableContraptionPacket.contraption());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(GetSableContraptionPacket packet, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            SableContraptionClient.complete(packet.contraption().uuid(), packet.contraption());
        });
    }
}
