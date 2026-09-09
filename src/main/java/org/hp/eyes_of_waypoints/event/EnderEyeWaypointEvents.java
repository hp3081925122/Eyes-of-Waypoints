package org.hp.eyes_of_waypoints.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
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

// 捕获原版和已知第三方定位之眼的使用事件，并通过注册表 ID 实现可选兼容。
@Mod.EventBusSubscriber(modid = Eyes_of_waypoints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnderEyeWaypointEvents {
    private EnderEyeWaypointEvents() {
    }

    // 处理定位之眼右键事件，目标模组不存在时不会尝试加载目标模组的 Java 类。
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        LocatorEye locatorEye = LocatorEye.find(event.getItemStack());
        if (locatorEye == null) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 原版在已瞄准末地传送门框架时会把末影之眼交给方块交互逻辑处理。
        if (locatorEye == LocatorEye.VANILLA && isTargetingEndPortalFrame(serverLevel, player)) {
            return;
        }

        // 所有定位之眼都使用与原版一致的 100 格结构搜索范围。
        BlockPos target = serverLevel.findNearestMapStructure(
                locatorEye.structureTag,
                player.blockPosition(),
                100,
                false
        );
        if (target == null) {
            return;
        }

        // 将服务端找到的结构坐标和路径点显示信息发给当前玩家的客户端。
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new EnderEyeWaypointPacket(target, locatorEye.nameKey, locatorEye.symbol, locatorEye.colorName)
        );
        Eyes_of_waypoints.LOGGER.debug(
                "Detected {} target at x={}, y={}, z={}, dimension={}",
                locatorEye.debugName,
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

    // 创建第三方结构标签，不引用第三方模组的类或加载器注册对象。
    private static TagKey<Structure> structureTag(String namespace, String path) {
        return TagKey.create(Registries.STRUCTURE, new ResourceLocation(namespace, path));
    }

    // 创建第三方物品的注册表 ID，目标模组缺失时该 ID 只会匹配不到物品。
    private static ResourceLocation itemId(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    // 用注册表中的实际物品 ID 匹配手持物品，避免直接链接可选模组的 Item 类。
    private enum LocatorEye {
        VANILLA(
                itemId("minecraft", "ender_eye"),
                StructureTags.EYE_OF_ENDER_LOCATED,
                "waypoint.eyes_of_waypoints.stronghold",
                "E",
                "YELLOW",
                "vanilla Eye of Ender"
        ),
        CATACLYSM_MECH(
                itemId("cataclysm", "mech_eye"),
                structureTag("cataclysm", "eye_of_mech_located"),
                "waypoint.eyes_of_waypoints.ancient_factory",
                "M",
                "RED",
                "Cataclysm Mech Eye"
        ),
        CATACLYSM_FLAME(
                itemId("cataclysm", "flame_eye"),
                structureTag("cataclysm", "eye_of_flame_located"),
                "waypoint.eyes_of_waypoints.burning_arena",
                "F",
                "GOLD",
                "Cataclysm Flame Eye"
        ),
        CATACLYSM_VOID(
                itemId("cataclysm", "void_eye"),
                structureTag("cataclysm", "eye_of_ruined_located"),
                "waypoint.eyes_of_waypoints.ruined_citadel",
                "V",
                "PURPLE",
                "Cataclysm Void Eye"
        ),
        CATACLYSM_MONSTROUS(
                itemId("cataclysm", "monstrous_eye"),
                structureTag("cataclysm", "eye_of_monstrous_located"),
                "waypoint.eyes_of_waypoints.soul_black_smith",
                "O",
                "DARK_GRAY",
                "Cataclysm Monstrous Eye"
        ),
        CATACLYSM_ABYSS(
                itemId("cataclysm", "abyss_eye"),
                structureTag("cataclysm", "eye_of_abyss_located"),
                "waypoint.eyes_of_waypoints.sunken_city",
                "A",
                "DARK_PURPLE",
                "Cataclysm Abyss Eye"
        ),
        CATACLYSM_DESERT(
                itemId("cataclysm", "desert_eye"),
                structureTag("cataclysm", "eye_of_desert_located"),
                "waypoint.eyes_of_waypoints.cursed_pyramid",
                "D",
                "BROWN",
                "Cataclysm Desert Eye"
        ),
        CATACLYSM_CURSED(
                itemId("cataclysm", "cursed_eye"),
                structureTag("cataclysm", "eye_of_curse_located"),
                "waypoint.eyes_of_waypoints.frosted_prison",
                "C",
                "DARK_AQUA",
                "Cataclysm Cursed Eye"
        ),
        CATACLYSM_STORM(
                itemId("cataclysm", "storm_eye"),
                structureTag("cataclysm", "eye_of_storm_located"),
                "waypoint.eyes_of_waypoints.acropolis",
                "S",
                "LIGHT_BLUE",
                "Cataclysm Storm Eye"
        ),
        LEGENDARY_MANY_RIBS(
                itemId("legendary_monsters", "eye_of_many_ribs"),
                structureTag("legendary_monsters", "ancient_stronghold_eye_located"),
                "waypoint.eyes_of_waypoints.ancient_stronghold",
                "R",
                "DARK_PURPLE",
                "Legendary Monsters Eye of Many Ribs"
        ),
        LEGENDARY_FROST(
                itemId("legendary_monsters", "eye_of_frost"),
                structureTag("legendary_monsters", "frostbitten_temple_eye_located"),
                "waypoint.eyes_of_waypoints.frostbitten_temple",
                "F",
                "LIGHT_BLUE",
                "Legendary Monsters Eye of Frost"
        ),
        LEGENDARY_AIR(
                itemId("legendary_monsters", "eye_of_air"),
                structureTag("legendary_monsters", "cloudy_temple_eye_located"),
                "waypoint.eyes_of_waypoints.cloudy_temple",
                "A",
                "WHITE",
                "Legendary Monsters Eye of Air"
        ),
        LEGENDARY_MOSS(
                itemId("legendary_monsters", "eye_of_moss"),
                structureTag("legendary_monsters", "mossy_temple_eye_located"),
                "waypoint.eyes_of_waypoints.mossy_temple",
                "M",
                "GREEN",
                "Legendary Monsters Eye of Moss"
        ),
        LEGENDARY_SHULKER(
                itemId("legendary_monsters", "eye_of_shulker"),
                structureTag("legendary_monsters", "shulker_tower_eye_located"),
                "waypoint.eyes_of_waypoints.shulker_tower",
                "S",
                "PURPLE",
                "Legendary Monsters Eye of Shulker"
        ),
        LEGENDARY_CHORUS(
                itemId("legendary_monsters", "eye_of_chorus"),
                structureTag("legendary_monsters", "ancient_tower_remains_eye_located"),
                "waypoint.eyes_of_waypoints.ancient_tower_remains",
                "T",
                "DARK_PURPLE",
                "Legendary Monsters Eye of Chorus"
        ),
        LEGENDARY_MAGMA(
                itemId("legendary_monsters", "eye_of_magma"),
                structureTag("legendary_monsters", "lava_eater_spawn_eye_located"),
                "waypoint.eyes_of_waypoints.lava_eater_spawn",
                "L",
                "RED",
                "Legendary Monsters Eye of Magma"
        ),
        LEGENDARY_BONES(
                itemId("legendary_monsters", "eye_of_bones"),
                structureTag("legendary_monsters", "skeletosaurus_nest_eye_located"),
                "waypoint.eyes_of_waypoints.skeletosaurus_nest",
                "B",
                "GRAY",
                "Legendary Monsters Eye of Bones"
        ),
        LEGENDARY_SOUL(
                itemId("legendary_monsters", "eye_of_soul"),
                structureTag("legendary_monsters", "soul_fortress_remains_eye_located"),
                "waypoint.eyes_of_waypoints.soul_fortress_remains",
                "O",
                "DARK_GRAY",
                "Legendary Monsters Eye of Soul"
        ),
        LEGENDARY_GHOST(
                itemId("legendary_monsters", "eye_of_ghost"),
                structureTag("legendary_monsters", "collapsed_kingdom_eye_located"),
                "waypoint.eyes_of_waypoints.collapsed_kingdom",
                "G",
                "GRAY",
                "Legendary Monsters Eye of Ghost"
        ),
        LEGENDARY_ANNIHILATION(
                itemId("legendary_monsters", "eye_of_annihilation"),
                structureTag("legendary_monsters", "space_station_eye_located"),
                "waypoint.eyes_of_waypoints.space_station",
                "X",
                "AQUA",
                "Legendary Monsters Eye of Annihilation"
        ),
        LEGENDARY_SANDSTORM(
                itemId("legendary_monsters", "eye_of_sandstorm"),
                structureTag("legendary_monsters", "ruined_pyramid_eye_located"),
                "waypoint.eyes_of_waypoints.ruined_pyramid",
                "D",
                "GOLD",
                "Legendary Monsters Eye of Sandstorm"
        ),
        EEEAB_BLOODY_ALTAR(
                itemId("eeeabsmobs", "bloody_altar_eye"),
                structureTag("eeeabsmobs", "eye_of_bloody_altar"),
                "waypoint.eyes_of_waypoints.bloody_altar",
                "B",
                "DARK_RED",
                "EEEAB's Mobs Bloody Altar Eye"
        ),
        EEEAB_COREFORGE_RUINS(
                itemId("eeeabsmobs", "coreforge_ruins_eye"),
                structureTag("eeeabsmobs", "eye_of_coreforge_ruins"),
                "waypoint.eyes_of_waypoints.coreforge_ruins",
                "C",
                "AQUA",
                "EEEAB's Mobs Coreforge Ruins Eye"
        );

        private final ResourceLocation itemId;
        private final TagKey<Structure> structureTag;
        private final String nameKey;
        private final String symbol;
        private final String colorName;
        private final String debugName;

        // 保存单种定位之眼的注册表 ID、结构标签和路径点显示信息。
        LocatorEye(
                ResourceLocation itemId,
                TagKey<Structure> structureTag,
                String nameKey,
                String symbol,
                String colorName,
                String debugName
        ) {
            this.itemId = itemId;
            this.structureTag = structureTag;
            this.nameKey = nameKey;
            this.symbol = symbol;
            this.colorName = colorName;
            this.debugName = debugName;
        }

        // 根据物品注册表 ID 识别定位之眼，缺少对应模组时自然不会匹配。
        private static LocatorEye find(ItemStack itemStack) {
            ResourceLocation heldItemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            for (LocatorEye candidate : values()) {
                if (candidate.itemId.equals(heldItemId)) {
                    return candidate;
                }
            }
            return null;
        }
    }
}
