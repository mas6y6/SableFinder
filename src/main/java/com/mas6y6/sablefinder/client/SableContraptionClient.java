package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.networking.GetSableContraptionListPacket;
import com.mas6y6.sablefinder.networking.GetSableContraptionPacket;
import com.mas6y6.sablefinder.networking.PacketRequests;
import com.mas6y6.sablefinder.networking.SableContraptionListRequestPacket;
import com.mas6y6.sablefinder.networking.SableContraptionRequestPacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public final class SableContraptionClient {
    private SableContraptionClient() {
    }

    public static CompletableFuture<SableContraptionData> request(UUID contraptionId) {
        return PacketRequests.sendToServer(new SableContraptionRequestPacket(PacketRequests.newId(), contraptionId))
                .thenApply(GetSableContraptionPacket::contraption);
    }

    public static CompletableFuture<CompoundTag> requestContraptionList() {
        return PacketRequests.sendToServer(new SableContraptionListRequestPacket(PacketRequests.newId()))
                .thenApply(GetSableContraptionListPacket::sableContraptions);
    }
}
