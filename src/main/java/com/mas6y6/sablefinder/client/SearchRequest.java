package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.networking.CancelSearchPacket;
import com.mas6y6.sablefinder.networking.PacketRequests;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SearchRequest {
    private final UUID requestId;
    private final CompletableFuture<CompoundTag> future;

    SearchRequest(UUID requestId, CompletableFuture<CompoundTag> future) {
        this.requestId = requestId;
        this.future = future;
    }

    public UUID requestId() {
        return requestId;
    }

    public CompletableFuture<CompoundTag> future() {
        return future;
    }

    public void cancel() {
        PacketRequests.cancel(requestId);

        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new CancelSearchPacket(requestId));
        }
    }
}
