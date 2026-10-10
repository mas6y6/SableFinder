package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.sable.SableUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SableContraptionListRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SableContraptionListRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "request_contraption_list"));

    public static final StreamCodec<ByteBuf, SableContraptionListRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new SableContraptionListRequestPacket()
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(SableContraptionListRequestPacket packet, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (iPayloadContext.player() instanceof ServerPlayer player) {
                try {
                    final MinecraftServer server = player.serverLevel().getServer();
                    final CompoundTag tag = SableUtils.buildContraptionsTag(server);
                    iPayloadContext.reply(new GetSableContraptionListPacket(tag));
                } catch (Exception e) {
                    SableFinder.LOGGER.error("An error occurred while loading contraption list for player {}", player.getName().getString(), e);
                }
            }
        });
    }
}
