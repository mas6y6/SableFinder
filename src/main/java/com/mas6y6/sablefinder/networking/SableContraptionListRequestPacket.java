package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
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

public record SableContraptionListRequestPacket(UUID requestId) implements RequestPayload<SableContraptionListResponsePacket> {
    public static final CustomPacketPayload.Type<SableContraptionListRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "request_contraption_list"));

    public static final StreamCodec<ByteBuf, SableContraptionListRequestPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SableContraptionListRequestPacket::requestId,
            SableContraptionListRequestPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public CustomPacketPayload.@NotNull Type<SableContraptionListResponsePacket> responseType() {
        return SableContraptionListResponsePacket.TYPE;
    }

    public static void handleServer(@NotNull SableContraptionListRequestPacket packet, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (iPayloadContext.player() instanceof ServerPlayer player) {
                try {
                    final MinecraftServer server = player.serverLevel().getServer();
                    SableUtils.buildContraptionsTagAsync(server).whenComplete((tag, error) -> {
                        if (error != null) {
                            SableFinder.LOGGER.error("An error occurred while loading contraption list for player {}", player.getName().getString(), error);
                            return;
                        }

                        if (server.isRunning()) {
                            server.execute(() -> iPayloadContext.reply(new SableContraptionListResponsePacket(packet.requestId(), tag)));
                        }
                    });
                } catch (Exception e) {
                    SableFinder.LOGGER.error("An error occurred while loading contraption list for player {}", player.getName().getString(), e);
                }
            }
        });
    }
}
