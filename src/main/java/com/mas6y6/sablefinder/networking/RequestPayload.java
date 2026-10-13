package com.mas6y6.sablefinder.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public interface RequestPayload<R extends CustomPacketPayload> extends CustomPacketPayload {
    UUID requestId();

    CustomPacketPayload.Type<R> responseType();
}
