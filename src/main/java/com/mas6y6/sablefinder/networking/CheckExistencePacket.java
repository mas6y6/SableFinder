package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public record CheckExistencePacket(boolean isServerHasMod, boolean isClientHasMod) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CheckExistencePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "check_existence"));

    public static final StreamCodec<ByteBuf, CheckExistencePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            CheckExistencePacket::isServerHasMod,
            ByteBufCodecs.BOOL,
            CheckExistencePacket::isClientHasMod,
            CheckExistencePacket::new
    );

    private static final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> PENDING_RESPONSES = new ConcurrentHashMap<>();

    public static CompletableFuture<Boolean> request(UUID playerId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        PENDING_RESPONSES.put(playerId, future);
        return future;
    }

    public static void removePending(UUID playerId) {
        PENDING_RESPONSES.remove(playerId);
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(CheckExistencePacket packet, IPayloadContext iPayloadContext) {
        CompletableFuture<Boolean> future = PENDING_RESPONSES.remove(iPayloadContext.player().getUUID());
        if (future != null) {
            future.complete(packet.isClientHasMod());
        }
    }

    public static void handleClient(CheckExistencePacket packet, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (packet.isServerHasMod()) {
                iPayloadContext.reply(new CheckExistencePacket(true, true));
            }
        });
    }
}
