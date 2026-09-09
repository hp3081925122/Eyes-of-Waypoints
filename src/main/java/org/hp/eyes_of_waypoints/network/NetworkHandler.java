package org.hp.eyes_of_waypoints.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;

// 集中注册本模组使用的 Forge 网络数据包。
public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "2";
    private static int nextPacketId;

    // 创建仅允许服务端发送到客户端的坐标同步通道。
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Eyes_of_waypoints.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private NetworkHandler() {
    }

    // 注册末影之眼目标坐标数据包，并限制其传输方向。
    public static void register() {
        CHANNEL.messageBuilder(EnderEyeWaypointPacket.class, nextPacketId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EnderEyeWaypointPacket::encode)
                .decoder(EnderEyeWaypointPacket::decode)
                .consumerMainThread(EnderEyeWaypointPacket::handle)
                .add();
    }
}
