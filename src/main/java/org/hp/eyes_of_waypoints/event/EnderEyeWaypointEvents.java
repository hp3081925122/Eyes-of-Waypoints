package org.hp.eyes_of_waypoints.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;
import org.hp.eyes_of_waypoints.network.EnderEyeWaypointPacket;
import org.hp.eyes_of_waypoints.network.NetworkHandler;

// 捕获原版末影之眼真正开始使用的服务端事件，并复用原版结构定位规则。
@Mod.EventBusSubscriber(modid = Eyes_of_waypoints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnderEyeWaypointEvents {
    private EnderEyeWaypointEvents() {
    }

    // 只有 RightClickItem 才会在方块交互未消费物品后触发，避免误判箱子等方块交互。
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !event.getItemStack().is(Items.ENDER_EYE)) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 原版在已瞄准末地传送门框架时会把末影之眼交给 useOn，不会发射定位之眼。
        if (isTargetingEndPortalFrame(serverLevel, player)) {
            return;
        }

        BlockPos target = serverLevel.findNearestMapStructure(
                StructureTags.EYE_OF_ENDER_LOCATED,
                player.blockPosition(),
                100,
                false
        );
        if (target == null) {
            return;
        }

        // 将服务端复用原版规则得到的坐标发给当前玩家的客户端。
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new EnderEyeWaypointPacket(target)
        );
        Eyes_of_waypoints.LOGGER.debug(
                "Detected vanilla Eye of Ender target at x={}, y={}, z={}, dimension={}",
                target.getX(),
                target.getY(),
                target.getZ(),
                serverLevel.dimension().location()
        );
    }

    // 使用原版相同的方块触达范围判断末影之眼是否正指向末地传送门框架。
    private static boolean isTargetingEndPortalFrame(ServerLevel level, Player player) {
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 0.5D;
        HitResult hitResult = player.pick(reach, 1.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        return level.getBlockState(blockHitResult.getBlockPos()).is(Blocks.END_PORTAL_FRAME);
    }
}
