package org.hp.eyes_of_waypoints.event;

import com.github.L_Ender.cataclysm.init.ModItems;
import com.github.L_Ender.cataclysm.init.ModTag;
import net.miauczel.legendary_monsters.tag.ModStructureTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;
import org.hp.eyes_of_waypoints.network.EnderEyeWaypointPayload;

import java.util.function.Supplier;

// 捕获原版、Cataclysm 与 Legendary Monsters 定位眼的使用事件，并复用对应物品的结构标签定位目标。
@EventBusSubscriber(modid = Eyes_of_waypoints.MODID)
public final class EnderEyeWaypointEvents {
    private EnderEyeWaypointEvents() {
    }

    // RightClickItem 只在方块和实体交互没有消费本次使用时触发，保持与定位眼 use 路径一致。
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LocatorEye locatorEye = null;
        for (LocatorEye candidate : LocatorEye.values()) {
            if (event.getItemStack().is(candidate.item.get())) {
                locatorEye = candidate;
                break;
            }
        }
        if (locatorEye == null) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 使用与原版及第三方定位眼一致的 100 格搜索范围查找结构。
        BlockPos target = serverLevel.findNearestMapStructure(
                locatorEye.structureTag,
                serverPlayer.blockPosition(),
                100,
                false
        );
        if (target == null) {
            return;
        }

        // 将服务端定位结果与对应显示信息发给当前玩家的客户端。
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new EnderEyeWaypointPayload(
                        target,
                        locatorEye.nameKey,
                        locatorEye.symbol,
                        locatorEye.colorName
                )
        );
        Eyes_of_waypoints.LOGGER.debug(
                "Detected {} target at x={}, y={}, z={}",
                locatorEye.nameKey,
                target.getX(),
                target.getY(),
                target.getZ()
        );
    }

    // 记录每种定位眼对应的物品、结构标签和 Xaero 显示字段。
    private enum LocatorEye {
        VANILLA(
                () -> net.minecraft.world.item.Items.ENDER_EYE,
                StructureTags.EYE_OF_ENDER_LOCATED,
                "waypoint.eyes_of_waypoints.stronghold",
                "E",
                "YELLOW"
        ),
        MECH(
                ModItems.MECH_EYE,
                ModTag.EYE_OF_MECH_LOCATED,
                "waypoint.eyes_of_waypoints.ancient_factory",
                "M",
                "RED"
        ),
        FLAME(
                ModItems.FLAME_EYE,
                ModTag.EYE_OF_FLAME_LOCATED,
                "waypoint.eyes_of_waypoints.burning_arena",
                "F",
                "GOLD"
        ),
        VOID(
                ModItems.VOID_EYE,
                ModTag.EYE_OF_RUINED_LOCATED,
                "waypoint.eyes_of_waypoints.ruined_citadel",
                "V",
                "PURPLE"
        ),
        MONSTROUS(
                ModItems.MONSTROUS_EYE,
                ModTag.EYE_OF_MONSTROUS_LOCATED,
                "waypoint.eyes_of_waypoints.soul_black_smith",
                "O",
                "DARK_GRAY"
        ),
        ABYSS(
                ModItems.ABYSS_EYE,
                ModTag.EYE_OF_ABYSS_LOCATED,
                "waypoint.eyes_of_waypoints.sunken_city",
                "A",
                "DARK_PURPLE"
        ),
        DESERT(
                ModItems.DESERT_EYE,
                ModTag.EYE_OF_DESERT_LOCATED,
                "waypoint.eyes_of_waypoints.cursed_pyramid",
                "D",
                "BROWN"
        ),
        CURSED(
                ModItems.CURSED_EYE,
                ModTag.EYE_OF_CURSE_LOCATED,
                "waypoint.eyes_of_waypoints.frosted_prison",
                "C",
                "DARK_AQUA"
        ),
        STORM(
                ModItems.STORM_EYE,
                ModTag.EYE_OF_STORM_LOCATED,
                "waypoint.eyes_of_waypoints.acropolis",
                "S",
                "LIGHT_BLUE"
        ),
        LEGENDARY_ANCIENT_STRONGHOLD(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_MANY_RIBS,
                ModStructureTags.ANCIENT_STRONGHOLD_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.ancient_stronghold",
                "R",
                "RED"
        ),
        LEGENDARY_FROSTBITTEN_TEMPLE(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_FROST,
                ModStructureTags.FROSTBITTEN_TEMPLE_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.frostbitten_temple",
                "F",
                "LIGHT_BLUE"
        ),
        LEGENDARY_CLOUDY_TEMPLE(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_AIR,
                ModStructureTags.CLOUDY_TEMPLE_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.cloudy_temple",
                "C",
                "WHITE"
        ),
        LEGENDARY_MOSSY_TEMPLE(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_MOSS,
                ModStructureTags.MOSSY_TEMPLE_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.mossy_temple",
                "M",
                "GREEN"
        ),
        LEGENDARY_SHULKER_TOWER(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_SHULKER,
                ModStructureTags.SHULKER_TOWER_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.shulker_tower",
                "S",
                "PURPLE"
        ),
        LEGENDARY_ANCIENT_TOWER_REMAINS(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_CHORUS,
                ModStructureTags.ANCIENT_TOWER_REMAINS_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.ancient_tower_remains",
                "T",
                "DARK_PURPLE"
        ),
        LEGENDARY_LAVA_EATER_SPAWN(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_MAGMA,
                ModStructureTags.LAVA_EATER_SPAWN_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.lava_eater_spawn",
                "L",
                "RED"
        ),
        LEGENDARY_SKELETOSAURUS_NEST(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_BONES,
                ModStructureTags.SKELETOSAURUS_NEST_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.skeletosaurus_nest",
                "B",
                "DARK_GRAY"
        ),
        LEGENDARY_SOUL_FORTRESS_REMAINS(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_SOUL,
                ModStructureTags.SOUL_FORTRESS_REMAINS_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.soul_fortress_remains",
                "O",
                "DARK_PURPLE"
        ),
        LEGENDARY_COLLAPSED_KINGDOM(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_GHOST,
                ModStructureTags.COLLAPSED_KINGDOM_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.collapsed_kingdom",
                "K",
                "GOLD"
        ),
        LEGENDARY_SPACE_STATION(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_ANNIHILATION,
                ModStructureTags.SPACE_STATION_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.space_station",
                "A",
                "LIGHT_BLUE"
        ),
        LEGENDARY_RUINED_PYRAMID(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_SANDSTORM,
                ModStructureTags.RUINED_PYRAMID_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.ruined_pyramid",
                "P",
                "GOLD"
        );

        private final Supplier<Item> item;
        private final TagKey<Structure> structureTag;
        private final String nameKey;
        private final String symbol;
        private final String colorName;

        // 保存单个定位眼的兼容信息。
        LocatorEye(
                Supplier<Item> item,
                TagKey<Structure> structureTag,
                String nameKey,
                String symbol,
                String colorName
        ) {
            this.item = item;
            this.structureTag = structureTag;
            this.nameKey = nameKey;
            this.symbol = symbol;
            this.colorName = colorName;
        }
    }
}
