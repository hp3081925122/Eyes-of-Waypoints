package org.hp.eyes_of_waypoints.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;
import org.hp.eyes_of_waypoints.network.EnderEyeWaypointPayload;

// 捕获原版末影之眼开始使用的服务端事件，并复用原版结构定位规则。
@EventBusSubscriber(modid = Eyes_of_waypoints.MODID)
public final class EnderEyeWaypointEvents {
    private EnderEyeWaypointEvents() {
    }

    // RightClickItem 只在方块和实体交互没有消费本次使用时触发，保持与原版末影之眼 use 路径一致。
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide() || !event.getItemStack().is(Items.ENDER_EYE)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos target = serverLevel.findNearestMapStructure(
                StructureTags.EYE_OF_ENDER_LOCATED,
                serverPlayer.blockPosition(),
                100,
                false
        );
        if (target == null) {
            return;
        }

        // 将服务端复用原版规则得到的坐标发给当前玩家的客户端。
        PacketDistributor.sendToPlayer(serverPlayer, new EnderEyeWaypointPayload(target));
        Eyes_of_waypoints.LOGGER.debug(
                "Detected vanilla Eye of Ender target at x={}, y={}, z={}",
                target.getX(),
                target.getY(),
                target.getZ()
        );
    }
}
