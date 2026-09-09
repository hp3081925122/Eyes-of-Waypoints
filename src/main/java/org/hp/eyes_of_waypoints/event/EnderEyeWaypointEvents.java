package org.hp.eyes_of_waypoints.event;

import com.github.L_Ender.cataclysm.init.ModItems;
import com.github.L_Ender.cataclysm.init.ModTag;
import com.eeeab.eeeabsmobs.sever.init.ItemInit;
import com.eeeab.eeeabsmobs.sever.util.ModTagKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;
import net.miauczel.legendary_monsters.tag.ModStructureTags;
import org.hp.eyes_of_waypoints.Eyes_of_waypoints;
import org.hp.eyes_of_waypoints.network.EnderEyeWaypointPacket;
import org.hp.eyes_of_waypoints.network.NetworkHandler;

// 捕获定位之眼开始使用的服务端事件，并复用各模组自己的结构定位规则。
@Mod.EventBusSubscriber(modid = Eyes_of_waypoints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnderEyeWaypointEvents {
    private EnderEyeWaypointEvents() {
    }

    // 只有 RightClickItem 才会在方块交互未消费物品后触发，避免误判容器等方块交互。
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        ItemStack itemStack = event.getItemStack();
        boolean vanillaEye = itemStack.is(Items.ENDER_EYE);
        CataclysmEye cataclysmEye = vanillaEye ? null : CataclysmEye.find(itemStack);
        LegendaryMonstersEye legendaryMonstersEye = vanillaEye || cataclysmEye != null
                ? null
                : LegendaryMonstersEye.find(itemStack);
        EeeabMobsEye eeeabMobsEye = vanillaEye || cataclysmEye != null || legendaryMonstersEye != null
                ? null
                : EeeabMobsEye.find(itemStack);
        if (!vanillaEye && cataclysmEye == null && legendaryMonstersEye == null && eeeabMobsEye == null) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 原版在已瞄准末地传送门框架时会把末影之眼交给 useOn，不会发射定位之眼。
        if (vanillaEye && isTargetingEndPortalFrame(serverLevel, player)) {
            return;
        }

        // 使用原版、Cataclysm 和 Legendary Monsters 定位之眼相同的搜索半径。
        TagKey<Structure> structureTag;
        String nameKey;
        String symbol;
        String colorName;
        String debugName;
        if (vanillaEye) {
            structureTag = StructureTags.EYE_OF_ENDER_LOCATED;
            nameKey = "waypoint.eyes_of_waypoints.stronghold";
            symbol = "E";
            colorName = "YELLOW";
            debugName = "vanilla Eye of Ender";
        } else if (cataclysmEye != null) {
            structureTag = cataclysmEye.structureTag;
            nameKey = cataclysmEye.nameKey;
            symbol = cataclysmEye.symbol;
            colorName = cataclysmEye.colorName;
            debugName = cataclysmEye.debugName;
        } else if (legendaryMonstersEye != null) {
            structureTag = legendaryMonstersEye.structureTag;
            nameKey = legendaryMonstersEye.nameKey;
            symbol = legendaryMonstersEye.symbol;
            colorName = legendaryMonstersEye.colorName;
            debugName = legendaryMonstersEye.debugName;
        } else {
            structureTag = eeeabMobsEye.structureTag;
            nameKey = eeeabMobsEye.nameKey;
            symbol = eeeabMobsEye.symbol;
            colorName = eeeabMobsEye.colorName;
            debugName = eeeabMobsEye.debugName;
        }

        BlockPos target = serverLevel.findNearestMapStructure(
                structureTag,
                player.blockPosition(),
                100,
                false
        );
        if (target == null) {
            return;
        }

        // 将服务端复用定位之眼规则得到的坐标发给当前玩家的客户端。
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new EnderEyeWaypointPacket(target, nameKey, symbol, colorName)
        );
        Eyes_of_waypoints.LOGGER.debug(
                "Detected {} target at x={}, y={}, z={}, dimension={}",
                debugName,
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

    // 保留 Cataclysm 八种定位之眼与其官方结构标签之间的一一对应关系。
    private enum CataclysmEye {
        MECH(ModItems.MECH_EYE, ModTag.EYE_OF_MECH_LOCATED, "waypoint.eyes_of_waypoints.ancient_factory", "M", "RED", "Mech Eye"),
        FLAME(ModItems.FLAME_EYE, ModTag.EYE_OF_FLAME_LOCATED, "waypoint.eyes_of_waypoints.burning_arena", "F", "GOLD", "Flame Eye"),
        VOID(ModItems.VOID_EYE, ModTag.EYE_OF_RUINED_LOCATED, "waypoint.eyes_of_waypoints.ruined_citadel", "V", "PURPLE", "Void Eye"),
        MONSTROUS(ModItems.MONSTROUS_EYE, ModTag.EYE_OF_MONSTROUS_LOCATED, "waypoint.eyes_of_waypoints.soul_black_smith", "O", "DARK_GRAY", "Monstrous Eye"),
        ABYSS(ModItems.ABYSS_EYE, ModTag.EYE_OF_ABYSS_LOCATED, "waypoint.eyes_of_waypoints.sunken_city", "A", "DARK_PURPLE", "Abyss Eye"),
        DESERT(ModItems.DESERT_EYE, ModTag.EYE_OF_DESERT_LOCATED, "waypoint.eyes_of_waypoints.cursed_pyramid", "D", "BROWN", "Desert Eye"),
        CURSED(ModItems.CURSED_EYE, ModTag.EYE_OF_CURSE_LOCATED, "waypoint.eyes_of_waypoints.frosted_prison", "C", "DARK_AQUA", "Cursed Eye"),
        STORM(ModItems.STORM_EYE, ModTag.EYE_OF_STORM_LOCATED, "waypoint.eyes_of_waypoints.acropolis", "S", "LIGHT_BLUE", "Storm Eye");

        private final RegistryObject<Item> item;
        private final TagKey<Structure> structureTag;
        private final String nameKey;
        private final String symbol;
        private final String colorName;
        private final String debugName;

        // 保存一个定位之眼对应的注册物品、结构标签和路径点显示信息。
        CataclysmEye(RegistryObject<Item> item, TagKey<Structure> structureTag, String nameKey, String symbol, String colorName, String debugName) {
            this.item = item;
            this.structureTag = structureTag;
            this.nameKey = nameKey;
            this.symbol = symbol;
            this.colorName = colorName;
            this.debugName = debugName;
        }

        // 根据实际物品注册对象识别当前右键使用的 Cataclysm 定位之眼。
        private static CataclysmEye find(ItemStack itemStack) {
            for (CataclysmEye eye : values()) {
                if (itemStack.is(eye.item.get())) {
                    return eye;
                }
            }
            return null;
        }
    }

    // 保留 Legendary Monsters 十二种定位之眼与其官方结构标签之间的一一对应关系。
    private enum LegendaryMonstersEye {
        MANY_RIBS(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_MANY_RIBS,
                ModStructureTags.ANCIENT_STRONGHOLD_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.ancient_stronghold", "R", "DARK_PURPLE", "Eye of Many Ribs"
        ),
        FROST(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_FROST,
                ModStructureTags.FROSTBITTEN_TEMPLE_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.frostbitten_temple", "F", "LIGHT_BLUE", "Eye of Frost"
        ),
        AIR(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_AIR,
                ModStructureTags.CLOUDY_TEMPLE_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.cloudy_temple", "A", "WHITE", "Eye of Air"
        ),
        MOSS(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_MOSS,
                ModStructureTags.MOSSY_TEMPLE_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.mossy_temple", "M", "GREEN", "Eye of Moss"
        ),
        SHULKER(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_SHULKER,
                ModStructureTags.SHULKER_TOWER_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.shulker_tower", "S", "PURPLE", "Eye of Shulker"
        ),
        CHORUS(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_CHORUS,
                ModStructureTags.ANCIENT_TOWER_REMAINS_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.ancient_tower_remains", "T", "DARK_PURPLE", "Eye of Chorus"
        ),
        MAGMA(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_MAGMA,
                ModStructureTags.LAVA_EATER_SPAWN_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.lava_eater_spawn", "L", "RED", "Eye of Magma"
        ),
        BONES(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_BONES,
                ModStructureTags.SKELETOSAURUS_NEST_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.skeletosaurus_nest", "B", "GRAY", "Eye of Bones"
        ),
        SOUL(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_SOUL,
                ModStructureTags.SOUL_FORTRESS_REMAINS_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.soul_fortress_remains", "O", "DARK_GRAY", "Eye of Soul"
        ),
        GHOST(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_GHOST,
                ModStructureTags.COLLAPSED_KINGDOM_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.collapsed_kingdom", "G", "GRAY", "Eye of Ghost"
        ),
        ANNIHILATION(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_ANNIHILATION,
                ModStructureTags.SPACE_STATION_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.space_station", "X", "AQUA", "Eye of Annihilation"
        ),
        SANDSTORM(
                net.miauczel.legendary_monsters.item.ModItems.EYE_OF_SANDSTORM,
                ModStructureTags.RUINED_PYRAMID_EYE_LOCATED,
                "waypoint.eyes_of_waypoints.ruined_pyramid", "D", "GOLD", "Eye of Sandstorm"
        );

        private final RegistryObject<Item> item;
        private final TagKey<Structure> structureTag;
        private final String nameKey;
        private final String symbol;
        private final String colorName;
        private final String debugName;

        // 保存一个定位之眼对应的注册物品、结构标签和路径点显示信息。
        LegendaryMonstersEye(RegistryObject<Item> item, TagKey<Structure> structureTag, String nameKey, String symbol, String colorName, String debugName) {
            this.item = item;
            this.structureTag = structureTag;
            this.nameKey = nameKey;
            this.symbol = symbol;
            this.colorName = colorName;
            this.debugName = debugName;
        }

        // 根据实际物品注册对象识别当前右键使用的 Legendary Monsters 定位之眼。
        private static LegendaryMonstersEye find(ItemStack itemStack) {
            for (LegendaryMonstersEye eye : values()) {
                if (itemStack.is(eye.item.get())) {
                    return eye;
                }
            }
            return null;
        }
    }

    // 保留 EEEAB's Mobs 两种定位之眼与其官方结构标签之间的一一对应关系。
    private enum EeeabMobsEye {
        BLOODY_ALTAR(
                ItemInit.BLOODY_ALTAR_EYE,
                ModTagKey.EYE_OF_BLOODY_ALTAR,
                "waypoint.eyes_of_waypoints.bloody_altar", "B", "DARK_RED", "Bloody Altar Eye"
        ),
        COREFORGE_RUINS(
                ItemInit.COREFORGE_RUINS_EYE,
                ModTagKey.EYE_OF_COREFORGE_RUINS,
                "waypoint.eyes_of_waypoints.coreforge_ruins", "C", "AQUA", "Coreforge Ruins Eye"
        );

        private final RegistryObject<Item> item;
        private final TagKey<Structure> structureTag;
        private final String nameKey;
        private final String symbol;
        private final String colorName;
        private final String debugName;

        // 保存一个定位之眼对应的注册物品、结构标签和路径点显示信息。
        EeeabMobsEye(RegistryObject<Item> item, TagKey<Structure> structureTag, String nameKey, String symbol, String colorName, String debugName) {
            this.item = item;
            this.structureTag = structureTag;
            this.nameKey = nameKey;
            this.symbol = symbol;
            this.colorName = colorName;
            this.debugName = debugName;
        }

        // 根据实际物品注册对象识别当前右键使用的 EEEAB's Mobs 定位之眼。
        private static EeeabMobsEye find(ItemStack itemStack) {
            for (EeeabMobsEye eye : values()) {
                if (itemStack.is(eye.item.get())) {
                    return eye;
                }
            }
            return null;
        }
    }
}
