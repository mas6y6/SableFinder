package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.sable.SableUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record DeleteContraptionPacket(UUID contraptionUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeleteContraptionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "delete_contraption"));

    public static final StreamCodec<ByteBuf, DeleteContraptionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    DeleteContraptionPacket::contraptionUUID,
                    DeleteContraptionPacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(DeleteContraptionPacket packet, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (iPayloadContext.player() instanceof ServerPlayer player ) {
                if (SableUtils.deleteContraption(player.server, packet.contraptionUUID())) {
                    PacketDistributor.sendToPlayer(player, new ClientMessagePacket(
                            Component.translatable("sablefinder.message.delete.title"),
                            Component.translatable("sablefinder.message.delete.success")
                    ));
                } else {
                    PacketDistributor.sendToPlayer(player, new ClientMessagePacket(
                            Component.translatable("sablefinder.message.delete.failed_title"),
                            Component.translatable("sablefinder.message.delete.failed")
                    ));
                }
            }
        });
    }
}
