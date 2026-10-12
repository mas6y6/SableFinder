package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Performs the mod-presence handshake in both directions. The server sends one to ask whether the
 * client has the mod; the client answers with the same packet carrying {@code isClientHasMod}.
 */
public record CheckExistencePacket(UUID requestId, boolean isServerHasMod, boolean isClientHasMod)
        implements RequestPayload<CheckExistencePacket>, ResponsePayload {
    public static final CustomPacketPayload.Type<CheckExistencePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "check_existence"));

    public static final StreamCodec<ByteBuf, CheckExistencePacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CheckExistencePacket::requestId,
            ByteBufCodecs.BOOL,
            CheckExistencePacket::isServerHasMod,
            ByteBufCodecs.BOOL,
            CheckExistencePacket::isClientHasMod,
            CheckExistencePacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public CustomPacketPayload.Type<CheckExistencePacket> responseType() {
        return TYPE;
    }

    public static void handleServer(CheckExistencePacket packet, IPayloadContext iPayloadContext) {
        PacketRequests.complete(packet);
    }

    public static void handleClient(CheckExistencePacket packet, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (packet.isServerHasMod()) {
                iPayloadContext.reply(new CheckExistencePacket(packet.requestId(), true, true));
            }
        });
    }
}
