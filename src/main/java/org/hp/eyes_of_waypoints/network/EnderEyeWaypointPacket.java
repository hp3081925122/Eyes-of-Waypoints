package org.hp.eyes_of_waypoints.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 传输原版末影之眼本次定位到的结构坐标。
public final class EnderEyeWaypointPacket {
    private final BlockPos target;

    // 创建一个不可修改目标坐标的数据包。
    public EnderEyeWaypointPacket(BlockPos target) {
        this.target = target;
    }

    // 读取客户端创建路径点所需的目标坐标。
    public BlockPos target() {
        return target;
    }

    // 将目标坐标写入 Forge 网络缓冲区。
    public static void encode(EnderEyeWaypointPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.target);
    }

    // 从 Forge 网络缓冲区还原目标坐标。
    public static EnderEyeWaypointPacket decode(FriendlyByteBuf buffer) {
        return new EnderEyeWaypointPacket(buffer.readBlockPos());
    }

    // 在主线程发布客户端处理事件，避免网络线程直接操作 Xaero 数据。
    public static void handle(EnderEyeWaypointPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new ReceivedEvent(packet.target)));
        context.setPacketHandled(true);
    }

    // 客户端收到坐标后由客户端 Xaero 集成监听此事件。
    public static final class ReceivedEvent extends Event {
        private final BlockPos target;

        private ReceivedEvent(BlockPos target) {
            this.target = target;
        }

        // 返回服务端复用原版末影之眼逻辑得到的坐标。
        public BlockPos getTarget() {
            return target;
        }
    }
}
