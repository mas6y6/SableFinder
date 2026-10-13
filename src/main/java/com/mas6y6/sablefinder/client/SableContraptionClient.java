package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.networking.GetSableContraptionListPacket;
import com.mas6y6.sablefinder.networking.GetSableContraptionPacket;
import com.mas6y6.sablefinder.networking.PacketRequests;
import com.mas6y6.sablefinder.networking.SableContraptionListRequestPacket;
import com.mas6y6.sablefinder.networking.SableContraptionRequestPacket;
import com.mas6y6.sablefinder.networking.SearchForContraptionsPacket;
import com.mas6y6.sablefinder.networking.SearchForContraptionsResponsePacket;
import com.mas6y6.sablefinder.networking.SetContraptionLockPacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.search.SearchFilters;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public final class SableContraptionClient {
    private SableContraptionClient() {
    }

    public static @NotNull CompletableFuture<SableContraptionData> request(UUID contraptionId) {
        return PacketRequests.sendToServer(new SableContraptionRequestPacket(PacketRequests.newId(), contraptionId))
                .thenApply(GetSableContraptionPacket::contraption);
    }

    public static @NotNull CompletableFuture<CompoundTag> requestContraptionList() {
        return PacketRequests.sendToServer(new SableContraptionListRequestPacket(PacketRequests.newId()))
                .thenApply(GetSableContraptionListPacket::sableContraptions);
    }

    public static @NotNull CompletableFuture<SableContraptionData> setLocked(UUID contraptionId, boolean locked) {
        return PacketRequests.sendToServer(new SetContraptionLockPacket(PacketRequests.newId(), contraptionId, locked))
                .thenApply(GetSableContraptionPacket::contraption);
    }

    /**
     * Runs a filtered search on the server and returns a handle for the result.
     * The handle keeps the request id so the search can be cancelled.
     */
    public static @NotNull SearchRequest search(SearchFilters filters) {
        final UUID requestId = PacketRequests.newId();

        final CompletableFuture<CompoundTag> future = PacketRequests
                .sendToServer(new SearchForContraptionsPacket(requestId, filters))
                .thenApply(SearchForContraptionsResponsePacket::contraptions);

        return new SearchRequest(requestId, future);
    }
}
