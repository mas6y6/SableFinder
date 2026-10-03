package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.networking.SableContraptionRequestPacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SableContraptionClient {
    private static final ConcurrentHashMap<UUID, CompletableFuture<SableContraptionData>> PENDING = new ConcurrentHashMap<>();

    private SableContraptionClient() {
    }

    public static CompletableFuture<SableContraptionData> request(UUID contraptionId) {
        CompletableFuture<SableContraptionData> future = new CompletableFuture<>();
        PENDING.put(contraptionId, future);
        PacketDistributor.sendToServer(new SableContraptionRequestPacket(contraptionId));
        return future;
    }

    public static void complete(UUID contraptionId, SableContraptionData data) {
        CompletableFuture<SableContraptionData> future = PENDING.remove(contraptionId);
        if (future != null) {
            future.complete(data);
        }
    }

    public static void removePending(UUID contraptionId) {
        PENDING.remove(contraptionId);
    }
}