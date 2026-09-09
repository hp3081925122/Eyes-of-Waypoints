package org.hp.eyes_of_waypoints;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.hp.eyes_of_waypoints.network.NetworkHandler;
import org.slf4j.Logger;

// 注册模组入口，并初始化客户端与服务端之间的坐标同步通道。
@Mod(Eyes_of_waypoints.MODID)
public final class Eyes_of_waypoints {
    public static final String MODID = "eyes_of_waypoints";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 注册末影之眼目标坐标的数据包。
    public Eyes_of_waypoints() {
        NetworkHandler.register();
    }
}
