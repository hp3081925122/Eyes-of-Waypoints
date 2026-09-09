package org.hp.eyes_of_waypoints.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;

// 传输定位眼对应的目标坐标与 Xaero 路径点显示信息。
public record EnderEyeWaypointPayload(BlockPos target, String nameKey, String symbol, String colorName)
        implements CustomPacketPayload {
    public static final Type<EnderEyeWaypointPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Eyes_of_waypoints.MODID, "ender_eye_waypoint")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EnderEyeWaypointPayload> STREAM_CODEC =
            CustomPacketPayload.codec(EnderEyeWaypointPayload::write, EnderEyeWaypointPayload::new);

    // 固定坐标并为网络字段提供安全默认值。
    public EnderEyeWaypointPayload {
        target = target == null ? BlockPos.ZERO : target.immutable();
        nameKey = nameKey == null || nameKey.isBlank()
                ? "waypoint.eyes_of_waypoints.stronghold"
                : nameKey;
        symbol = symbol == null || symbol.isBlank() ? "E" : symbol;
        colorName = colorName == null || colorName.isBlank() ? "YELLOW" : colorName;
    }

    // 保留原版末影之眼构造方式，默认显示要塞路径点。
    public EnderEyeWaypointPayload(BlockPos target) {
        this(target, "waypoint.eyes_of_waypoints.stronghold", "E", "YELLOW");
    }

    // 从网络缓冲区读取定位眼数据。
    private EnderEyeWaypointPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readUtf(128), buffer.readUtf(16), buffer.readUtf(32));
    }

    // 将定位眼数据写入 NeoForge 网络缓冲区。
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(target);
        buffer.writeUtf(nameKey, 128);
        buffer.writeUtf(symbol, 16);
        buffer.writeUtf(colorName, 32);
    }

    @Override
    public Type<EnderEyeWaypointPayload> type() {
        return TYPE;
    }

    // 客户端收到定位眼数据后由 Xaero 集成监听此事件。
    public static final class ReceivedEvent extends Event {
        private final BlockPos target;
        private final String nameKey;
        private final String symbol;
        private final String colorName;

        // 创建客户端事件并固定网络字段。
        public ReceivedEvent(BlockPos target, String nameKey, String symbol, String colorName) {
            this.target = target == null ? BlockPos.ZERO : target.immutable();
            this.nameKey = nameKey == null || nameKey.isBlank()
                    ? "waypoint.eyes_of_waypoints.stronghold"
                    : nameKey;
            this.symbol = symbol == null || symbol.isBlank() ? "E" : symbol;
            this.colorName = colorName == null || colorName.isBlank() ? "YELLOW" : colorName;
        }

        // 返回服务端定位到的结构坐标。
        public BlockPos target() {
            return target;
        }

        // 返回路径点本地化键。
        public String nameKey() {
            return nameKey;
        }

        // 返回 Xaero 路径点符号。
        public String symbol() {
            return symbol;
        }

        // 返回 Xaero 路径点颜色枚举名称。
        public String colorName() {
            return colorName;
        }
    }
}
