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
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 在客户端将服务端确认的末影之眼目标写入 26.1.2 Xaero's Minimap 当前世界。
@EventBusSubscriber(modid = Eyes_of_waypoints.MODID, value = Dist.CLIENT)
public final class XaeroWaypointHandler {
    private static final String WAYPOINT_SYMBOL = "E";
    private static final List<BlockPos> PENDING_TARGETS = new ArrayList<>();

    private XaeroWaypointHandler() {
    }

    // 接收网络事件，并在 Xaero 会话尚未完成初始化时保留坐标等待下一帧。
    @SubscribeEvent
    public static void onWaypointReceived(EnderEyeWaypointPayload.ReceivedEvent event) {
        if (!PENDING_TARGETS.contains(event.target())) {
            PENDING_TARGETS.add(event.target());
        }
        tryCreateWaypoints();
    }

    // 每客户端 tick 尝试处理待创建坐标，覆盖 Xaero 会话延迟初始化的情况。
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!PENDING_TARGETS.isEmpty()) {
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

        MinimapSession minimapSession = session.getMinimapProcessor() == null
                ? null
                : session.getMinimapProcessor().getSession();
        if (minimapSession == null) {
            return;
        }

        MinimapWorldManager worldManager = minimapSession.getWorldManager();
        MinimapWorld waypointWorld = worldManager == null ? null : worldManager.getCurrentWorld();
        if (waypointWorld == null) {
            return;
        }

        WaypointSet waypointSet = waypointWorld.getCurrentWaypointSet();
        if (waypointSet == null) {
            return;
        }

        for (int index = PENDING_TARGETS.size() - 1; index >= 0; index--) {
            BlockPos target = PENDING_TARGETS.get(index);
            if (!containsWaypoint(waypointWorld, target)) {
                String name = Component.translatable("waypoint.eyes_of_waypoints.stronghold").getString();
                Waypoint waypoint = new Waypoint(
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        name,
                        WAYPOINT_SYMBOL,
                        WaypointColor.YELLOW,
                        WaypointPurpose.NORMAL,
                        false,
                        false
                );
                waypointSet.add(waypoint);
                saveWaypoints(minimapSession, waypointWorld);
                Eyes_of_waypoints.LOGGER.debug(
                        "Created Xaero waypoint for vanilla Eye of Ender at x={}, y={}, z={}",
                        target.getX(),
                        target.getY(),
                        target.getZ()
                );
            }
            PENDING_TARGETS.remove(index);
        }
    }

    // 检查当前世界的所有路径点集合，避免同一要塞被重复添加。
    private static boolean containsWaypoint(MinimapWorld waypointWorld, BlockPos target) {
        for (WaypointSet waypointSet : waypointWorld.getIterableWaypointSets()) {
            for (Waypoint waypoint : waypointSet.getWaypoints()) {
                if (waypoint.getX() == target.getX()
                        && waypoint.getZ() == target.getZ()
                        && WAYPOINT_SYMBOL.equals(waypoint.getSymbol())) {
                    return true;
                }
            }
        }
        return false;
    }

    // 保存 Xaero 路径点文件，失败时记录英文日志并保留游戏运行。
    private static void saveWaypoints(MinimapSession minimapSession, MinimapWorld waypointWorld) {
        try {
            minimapSession.getWorldManagerIO().saveWorld(waypointWorld);
        } catch (IOException exception) {
            Eyes_of_waypoints.LOGGER.error("Failed to save Xaero waypoint", exception);
        }
    }
}
