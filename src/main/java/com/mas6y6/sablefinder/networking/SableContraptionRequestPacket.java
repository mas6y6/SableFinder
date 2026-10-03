package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.sable.SableUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SableContraptionRequestPacket(UUID contraptionId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SableContraptionRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "request_contraption"));

    public static final StreamCodec<ByteBuf, SableContraptionRequestPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SableContraptionRequestPacket::contraptionId,
            SableContraptionRequestPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(SableContraptionRequestPacket packet, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (iPayloadContext.player() instanceof ServerPlayer player) {
                final MinecraftServer server = player.serverLevel().getServer();
                final SableContraptionData contraption = SableUtils.getContraption(server, packet.contraptionId());
                if (contraption != null) {
                    iPayloadContext.reply(new GetSableContraptionPacket(contraption));
                }
            }
        });
    }
}