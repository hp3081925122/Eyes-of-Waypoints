package org.hp.eyes_of_waypoints.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 传输本次定位之眼找到的结构坐标和 Xaero 路径点显示信息。
public final class EnderEyeWaypointPacket {
    private final BlockPos target;
    private final String nameKey;
    private final String symbol;
    private final String colorName;

    // 创建原版末影之眼使用的默认路径点数据包。
    public EnderEyeWaypointPacket(BlockPos target) {
        this(target, "waypoint.eyes_of_waypoints.stronghold", "E", "YELLOW");
    }

    // 创建包含 Xaero 路径点名称、符号和颜色的数据包。
    public EnderEyeWaypointPacket(BlockPos target, String nameKey, String symbol, String colorName) {
        this.target = target;
        this.nameKey = nameKey;
        this.symbol = symbol;
        this.colorName = colorName;
    }

    // 读取客户端创建路径点所需的目标坐标。
    public BlockPos target() {
        return target;
    }

    // 返回客户端需要解析的路径点名称翻译键。
    public String nameKey() {
        return nameKey;
    }

    // 返回 Xaero 路径点符号。
    public String symbol() {
        return symbol;
    }

    // 返回 Xaero 路径点颜色枚举名。
    public String colorName() {
        return colorName;
    }

    // 将目标坐标和路径点显示信息写入 Forge 网络缓冲区。
    public static void encode(EnderEyeWaypointPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.target);
        buffer.writeUtf(packet.nameKey, 128);
        buffer.writeUtf(packet.symbol, 16);
        buffer.writeUtf(packet.colorName, 32);
    }

    // 从 Forge 网络缓冲区还原目标坐标和路径点显示信息。
    public static EnderEyeWaypointPacket decode(FriendlyByteBuf buffer) {
        return new EnderEyeWaypointPacket(
                buffer.readBlockPos(),
                buffer.readUtf(128),
                buffer.readUtf(16),
                buffer.readUtf(32)
        );
    }

    // 在主线程发布客户端处理事件，避免网络线程直接操作 Xaero 数据。
    public static void handle(EnderEyeWaypointPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new ReceivedEvent(
                packet.target,
                packet.nameKey,
                packet.symbol,
                packet.colorName
        )));
        context.setPacketHandled(true);
    }

    // 客户端收到定位结果后由客户端 Xaero 集成监听此事件。
    public static final class ReceivedEvent extends Event {
        private final BlockPos target;
        private final String nameKey;
        private final String symbol;
        private final String colorName;

        // 保存一次定位结果，供 Xaero 客户端集成读取。
        private ReceivedEvent(BlockPos target, String nameKey, String symbol, String colorName) {
            this.target = target;
            this.nameKey = nameKey;
            this.symbol = symbol;
            this.colorName = colorName;
        }

        // 返回服务端复用定位之眼逻辑得到的坐标。
        public BlockPos getTarget() {
            return target;
        }

        // 返回路径点名称翻译键。
        public String getNameKey() {
            return nameKey;
        }

        // 返回路径点符号。
        public String getSymbol() {
            return symbol;
        }

        // 返回路径点颜色枚举名。
        public String getColorName() {
            return colorName;
        }
    }
}
