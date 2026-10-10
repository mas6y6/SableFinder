package com.mas6y6.sablefinder.networking;

import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Networking {
    public static void registerPayloadHandlers(PayloadRegistrar payloadregister) {
        payloadregister.playBidirectional(
                CheckExistencePacket.TYPE,
                CheckExistencePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        CheckExistencePacket::handleClient,
                        CheckExistencePacket::handleServer
                )
        );

        payloadregister.playToClient(
                OpenGUIPacket.TYPE,
                OpenGUIPacket.STREAM_CODEC,
                OpenGUIPacket::handleClient
        );

        payloadregister.playToClient(
                GetSableContraptionPacket.TYPE,
                GetSableContraptionPacket.STREAM_CODEC,
                GetSableContraptionPacket::handleClient
        );

        payloadregister.playToServer(
                SableContraptionRequestPacket.TYPE,
                SableContraptionRequestPacket.STREAM_CODEC,
                SableContraptionRequestPacket::handleServer
        );

        payloadregister.playToClient(
                GetSableContraptionListPacket.TYPE,
                GetSableContraptionListPacket.STREAM_CODEC,
                GetSableContraptionListPacket::handleClient
        );

        payloadregister.playToServer(
                SableContraptionListRequestPacket.TYPE,
                SableContraptionListRequestPacket.STREAM_CODEC,
                SableContraptionListRequestPacket::handleServer
        );
    }
}
