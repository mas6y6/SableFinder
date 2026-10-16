package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.search.SearchEngine;
import com.mas6y6.sablefinder.search.SearchFilters;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SearchForContraptionsRequestPacket(UUID requestId, SearchFilters searchFilters) implements RequestPayload<SearchForContraptionsResponsePacket> {
    public static final CustomPacketPayload.Type<SearchForContraptionsRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "search_contraptions"));

    public static final StreamCodec<ByteBuf, SearchForContraptionsRequestPacket> STREAM_CODEC = StreamCodec.of(
            SearchForContraptionsRequestPacket::write,
            SearchForContraptionsRequestPacket::read
    );

    private static @NotNull SearchForContraptionsRequestPacket read(@NotNull ByteBuf byteBuf) {
        return new SearchForContraptionsRequestPacket(
                UUIDUtil.STREAM_CODEC.decode(byteBuf),
                SearchFilters.STREAM_CODEC.decode(byteBuf)
        );
    }

    private static void write(@NotNull ByteBuf byteBuf, @NotNull SearchForContraptionsRequestPacket packet) {
        UUIDUtil.STREAM_CODEC.encode(byteBuf, packet.requestId());
        SearchFilters.STREAM_CODEC.encode(byteBuf, packet.searchFilters());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public CustomPacketPayload.@NotNull Type<SearchForContraptionsResponsePacket> responseType() {
        return SearchForContraptionsResponsePacket.TYPE;
    }

    public void handleServer(@NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            SearchEngine.searchPacket(this, iPayloadContext);
        });
    }
}