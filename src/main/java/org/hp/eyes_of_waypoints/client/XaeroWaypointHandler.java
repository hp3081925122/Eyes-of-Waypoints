package org.hp.eyes_of_waypoints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;
import org.hp.eyes_of_waypoints.network.EnderEyeWaypointPayload;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 在客户端将服务端确认的定位眼目标写入 Xaero's Minimap 当前世界。
@EventBusSubscriber(modid = Eyes_of_waypoints.MODID, value = Dist.CLIENT)
public final class XaeroWaypointHandler {
    private static final List<EnderEyeWaypointPayload.ReceivedEvent> PENDING_REQUESTS = new ArrayList<>();

    private XaeroWaypointHandler() {
    }

    // 接收网络事件，并在 Xaero 会话尚未完成初始化时保留请求等待下一帧。
    @SubscribeEvent
    public static void onWaypointReceived(EnderEyeWaypointPayload.ReceivedEvent event) {
        boolean alreadyPending = PENDING_REQUESTS.stream().anyMatch(pending ->
                pending.target().equals(event.target())
                        && pending.symbol().equals(event.symbol())
                        && pending.nameKey().equals(event.nameKey())
        );
        if (!alreadyPending) {
            PENDING_REQUESTS.add(event);
        }
        tryCreateWaypoints();
    }

    // 每客户端 tick 尝试处理待创建请求，覆盖 Xaero 会话延迟初始化的情况。
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!PENDING_REQUESTS.isEmpty()) {
            tryCreateWaypoints();
        }
    }

    // 将待处理请求写入 Xaero 当前路径点集合并保存到磁盘。
    private static void tryCreateWaypoints() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
        if (session == null) {
            return;
        }

        WaypointsManager waypointsManager = session.getWaypointsManager();
        WaypointWorld waypointWorld = waypointsManager == null ? null : waypointsManager.getCurrentWorld();
        WaypointSet waypointSet = waypointWorld == null ? null : waypointWorld.getCurrentSet();
        if (waypointWorld == null || waypointSet == null) {
            return;
        }

        for (int index = PENDING_REQUESTS.size() - 1; index >= 0; index--) {
            EnderEyeWaypointPayload.ReceivedEvent request = PENDING_REQUESTS.get(index);
            if (!containsWaypoint(waypointWorld, request.target(), request.symbol())) {
                String name = Component.translatable(request.nameKey()).getString();
                Waypoint waypoint = new Waypoint(
                        request.target().getX(),
                        request.target().getY(),
                        request.target().getZ(),
                        name,
                        request.symbol(),
                        parseColor(request.colorName()),
                        WaypointPurpose.NORMAL,
                        false,
                        false
                );
                waypointSet.add(waypoint);
                saveWaypoints(waypointsManager, waypointWorld);
                Eyes_of_waypoints.LOGGER.debug(
                        "Created Xaero waypoint for {} at x={}, y={}, z={}",
                        request.nameKey(),
                        request.target().getX(),
                        request.target().getY(),
                        request.target().getZ()
                );
            }
            PENDING_REQUESTS.remove(index);
        }
    }

    // 将网络中的颜色名称转换为 Xaero 颜色，未知值回退到黄色。
    private static WaypointColor parseColor(String colorName) {
        try {
            return WaypointColor.valueOf(colorName);
        } catch (IllegalArgumentException exception) {
            Eyes_of_waypoints.LOGGER.warn("Unknown Xaero waypoint color: {}", colorName);
            return WaypointColor.YELLOW;
        }
    }

    // 检查当前世界的所有路径点集合，避免同一结构被重复添加。
    private static boolean containsWaypoint(WaypointWorld waypointWorld, BlockPos target, String symbol) {
        for (WaypointSet waypointSet : waypointWorld.getSets().values()) {
            for (Waypoint waypoint : waypointSet.getWaypoints()) {
                if (waypoint.getX() == target.getX()
                        && waypoint.getZ() == target.getZ()
                        && symbol.equals(waypoint.getSymbol())) {
                    return true;
                }
            }
        }
        return false;
    }

    // 保存 Xaero 路径点文件，失败时记录英文日志并保留游戏运行。
    private static void saveWaypoints(WaypointsManager waypointsManager, WaypointWorld waypointWorld) {
        try {
            waypointsManager.getWorldManagerIO().saveWorld(waypointWorld);
        } catch (IOException exception) {
            Eyes_of_waypoints.LOGGER.error("Failed to save Xaero waypoint", exception);
        }
    }
}
