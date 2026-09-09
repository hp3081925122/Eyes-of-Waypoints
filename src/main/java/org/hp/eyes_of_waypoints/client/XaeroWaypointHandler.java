package org.hp.eyes_of_waypoints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;
import org.hp.eyes_of_waypoints.network.EnderEyeWaypointPacket;
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

// 在客户端将服务端确认的定位之眼目标写入 Xaero's Minimap 当前世界。
@Mod.EventBusSubscriber(modid = Eyes_of_waypoints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class XaeroWaypointHandler {
    private static final List<EnderEyeWaypointPacket.ReceivedEvent> PENDING_REQUESTS = new ArrayList<>();

    private XaeroWaypointHandler() {
    }

    // 接收网络事件，并在 Xaero 会话尚未完成初始化时保留坐标等待下一帧。
    @SubscribeEvent
    public static void onWaypointReceived(EnderEyeWaypointPacket.ReceivedEvent event) {
        if (!containsPendingRequest(event)) {
            PENDING_REQUESTS.add(event);
        }
        tryCreateWaypoints();
    }

    // 每客户端帧尝试处理待创建坐标，覆盖 Xaero 会话延迟初始化的情况。
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !PENDING_REQUESTS.isEmpty()) {
            tryCreateWaypoints();
        }
    }

    // 将待处理坐标写入 Xaero 当前路径点集合并保存到磁盘。
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
            EnderEyeWaypointPacket.ReceivedEvent request = PENDING_REQUESTS.get(index);
            BlockPos target = request.getTarget();
            String symbol = request.getSymbol();
            if (!containsWaypoint(waypointWorld, target, symbol)) {
                String name = Component.translatable(request.getNameKey()).getString();
                Waypoint waypoint = new Waypoint(
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        name,
                        symbol,
                        parseColor(request.getColorName()),
                        WaypointPurpose.NORMAL,
                        false,
                        false
                );
                waypointSet.add(waypoint);
                saveWaypoints(waypointsManager, waypointWorld);
                Eyes_of_waypoints.LOGGER.debug(
                        "Created Xaero waypoint '{}' at x={}, y={}, z={}",
                        request.getNameKey(),
                        target.getX(),
                        target.getY(),
                        target.getZ()
                );
            }
            PENDING_REQUESTS.remove(index);
        }
    }

    // 检查待处理列表，避免同一次定位结果重复加入队列。
    private static boolean containsPendingRequest(EnderEyeWaypointPacket.ReceivedEvent request) {
        for (EnderEyeWaypointPacket.ReceivedEvent pending : PENDING_REQUESTS) {
            if (pending.getTarget().equals(request.getTarget())
                    && pending.getSymbol().equals(request.getSymbol())
                    && pending.getNameKey().equals(request.getNameKey())) {
                return true;
            }
        }
        return false;
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

    // 将网络包中的颜色名称转换为 Xaero 颜色，未知值回退到黄色。
    private static WaypointColor parseColor(String colorName) {
        try {
            return WaypointColor.valueOf(colorName);
        } catch (IllegalArgumentException exception) {
            Eyes_of_waypoints.LOGGER.warn("Unknown Xaero waypoint color '{}', using YELLOW", colorName);
            return WaypointColor.YELLOW;
        }
    }

    // 保存 Xaero 路径点文件，保存失败时只记录英文日志并保留游戏运行。
    private static void saveWaypoints(WaypointsManager waypointsManager, WaypointWorld waypointWorld) {
        try {
            waypointsManager.getWorldManagerIO().saveWorld(waypointWorld);
        } catch (IOException exception) {
            Eyes_of_waypoints.LOGGER.error("Failed to save Xaero waypoint", exception);
        }
    }
}
