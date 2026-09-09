package org.hp.eyes_of_waypoints.network;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;

// 集中注册 NeoForge 的客户端目标坐标负载。
public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {
    }

    // 注册仅由服务端发送到客户端的末影之眼目标坐标负载。
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                EnderEyeWaypointPayload.TYPE,
                EnderEyeWaypointPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> NeoForge.EVENT_BUS.post(
                        new EnderEyeWaypointPayload.ReceivedEvent(payload.target())
                ))
        );
    }
}
