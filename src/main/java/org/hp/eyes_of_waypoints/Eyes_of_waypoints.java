package org.hp.eyes_of_waypoints;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.hp.eyes_of_waypoints.network.NetworkHandler;
import org.slf4j.Logger;

// 注册模组入口，并把网络负载注册到 NeoForge 模组事件总线。
@Mod(Eyes_of_waypoints.MODID)
public final class Eyes_of_waypoints {
    public static final String MODID = "eyes_of_waypoints";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 注册服务端到客户端的末影之眼目标坐标负载。
    public Eyes_of_waypoints(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NetworkHandler::register);
    }
}
