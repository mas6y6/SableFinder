package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.networking.SableContraptionListRequestPacket;
import com.mas6y6.sablefinder.networking.SableContraptionRequestPacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class SableContraptionClient {
    private static final ConcurrentHashMap<UUID, CompletableFuture<SableContraptionData>> PENDING = new ConcurrentHashMap<>();
    private static final AtomicReference<CompletableFuture<CompoundTag>> PENDING_LIST = new AtomicReference<>();

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

    public static CompletableFuture<CompoundTag> requestContraptionList() {
        CompletableFuture<CompoundTag> future = new CompletableFuture<>();
        CompletableFuture<CompoundTag> old = PENDING_LIST.getAndSet(future);
        if (old != null && !old.isDone()) {
            old.cancel(true);
        }
        PacketDistributor.sendToServer(new SableContraptionListRequestPacket());
        return future;
    }

    public static void completeContraptionList(CompoundTag data) {
        CompletableFuture<CompoundTag> future = PENDING_LIST.getAndSet(null);
        if (future != null) {
            future.complete(data);
        }
    }

    public static void clearPendingList() {
        CompletableFuture<CompoundTag> future = PENDING_LIST.getAndSet(null);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }
}