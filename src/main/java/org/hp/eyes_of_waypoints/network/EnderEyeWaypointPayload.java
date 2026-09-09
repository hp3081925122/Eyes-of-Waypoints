package org.hp.eyes_of_waypoints.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;

// 传输服务端复用原版末影之眼规则得到的目标坐标。
public record EnderEyeWaypointPayload(BlockPos target) implements CustomPacketPayload {
    public static final Type<EnderEyeWaypointPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Eyes_of_waypoints.MODID, "ender_eye_waypoint")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EnderEyeWaypointPayload> STREAM_CODEC =
            CustomPacketPayload.codec(EnderEyeWaypointPayload::write, EnderEyeWaypointPayload::new);

    // 固定坐标对象，避免把可变坐标实例交给异步客户端处理。
    public EnderEyeWaypointPayload {
        target = target.immutable();
    }

    // 从网络缓冲区读取目标坐标。
    private EnderEyeWaypointPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    // 将目标坐标写入 NeoForge 网络缓冲区。
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(target);
    }

    @Override
    public Type<EnderEyeWaypointPayload> type() {
        return TYPE;
    }

    // 客户端收到坐标后由 Xaero 集成监听此事件。
    public static final class ReceivedEvent extends Event {
        private final BlockPos target;

        // 创建客户端事件并固定目标坐标。
        public ReceivedEvent(BlockPos target) {
            this.target = target.immutable();
        }

        // 返回服务端定位到的结构坐标。
        public BlockPos target() {
            return target;
        }
    }
}
