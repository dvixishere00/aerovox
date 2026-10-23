package com.aerovox;

import com.aerovox.assembly.AssemblyToolItem;
import com.aerovox.glue.SuperglueItem;
import com.aerovox.vehicle.AerovoxVehicle;
import com.aerovox.vehicle.BombEntity;
import com.aerovox.vehicle.WindsailBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AerovoxRegistries {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AerovoxMod.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AerovoxMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AerovoxMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AerovoxMod.MODID);

    public static final DeferredItem<SuperglueItem> SUPERGLUE = ITEMS.registerItem("superglue", SuperglueItem::new);
    public static final DeferredItem<AssemblyToolItem> ASSEMBLY_TOOL = ITEMS.registerItem("assembly_tool", AssemblyToolItem::new);
    public static final DeferredItem<Item> CONTROLLER_REMOTE = ITEMS.registerItem("controller_remote", Item::new);

    private static BlockBehaviour.Properties props(float strength, SoundType sound) {
        return BlockBehaviour.Properties.of().strength(strength).sound(sound);
    }

    public static final DeferredBlock<Block> ENGINE = BLOCKS.registerSimpleBlock("engine", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> CONTROL_SURFACE = BLOCKS.registerSimpleBlock("control_surface", props(2f, SoundType.COPPER));
    public static final DeferredBlock<Block> PILOT_SEAT = BLOCKS.registerSimpleBlock("pilot_seat", props(2f, SoundType.WOOD));
    public static final DeferredBlock<Block> CONTROLLER = BLOCKS.registerSimpleBlock("controller", props(2f, SoundType.METAL));
    public static final DeferredBlock<Block> SMALL_BOMB_MOUNT = BLOCKS.registerSimpleBlock("small_bomb_mount", props(2f, SoundType.METAL));
    public static final DeferredBlock<Block> BOMB_MOUNT = BLOCKS.registerSimpleBlock("bomb_mount", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> BIG_BOMB_MOUNT = BLOCKS.registerSimpleBlock("big_bomb_mount", props(4f, SoundType.METAL));
    public static final DeferredBlock<Block> LANDING_GEAR = BLOCKS.registerSimpleBlock("landing_gear", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> MOTOR = BLOCKS.registerSimpleBlock("motor", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> MOTOR_ROTATOR = BLOCKS.registerSimpleBlock("motor_rotator", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> REVERSED_MOTOR_ROTATOR = BLOCKS.registerSimpleBlock("reversed_motor_rotator", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> WINDSAIL_WORKER = BLOCKS.registerSimpleBlock("windsail_worker", props(3f, SoundType.METAL));
    public static final DeferredBlock<Block> INVERTED_WINDSAIL_WORKER = BLOCKS.registerSimpleBlock("inverted_windsail_worker", props(3f, SoundType.METAL));
    public static final DeferredBlock<WindsailBlock> WINDSAIL = BLOCKS.register("windsail",
            () -> new WindsailBlock(props(1f, SoundType.WOOL)));

    public static final DeferredItem<BlockItem> ENGINE_ITEM = ITEMS.registerSimpleBlockItem("engine", ENGINE);
    public static final DeferredItem<BlockItem> CONTROL_SURFACE_ITEM = ITEMS.registerSimpleBlockItem("control_surface", CONTROL_SURFACE);
    public static final DeferredItem<BlockItem> PILOT_SEAT_ITEM = ITEMS.registerSimpleBlockItem("pilot_seat", PILOT_SEAT);
    public static final DeferredItem<BlockItem> CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem("controller", CONTROLLER);
    public static final DeferredItem<BlockItem> SMALL_BOMB_MOUNT_ITEM = ITEMS.registerSimpleBlockItem("small_bomb_mount", SMALL_BOMB_MOUNT);
    public static final DeferredItem<BlockItem> BOMB_MOUNT_ITEM = ITEMS.registerSimpleBlockItem("bomb_mount", BOMB_MOUNT);
    public static final DeferredItem<BlockItem> BIG_BOMB_MOUNT_ITEM = ITEMS.registerSimpleBlockItem("big_bomb_mount", BIG_BOMB_MOUNT);
    public static final DeferredItem<BlockItem> LANDING_GEAR_ITEM = ITEMS.registerSimpleBlockItem("landing_gear", LANDING_GEAR);
    public static final DeferredItem<BlockItem> MOTOR_ITEM = ITEMS.registerSimpleBlockItem("motor", MOTOR);
    public static final DeferredItem<BlockItem> MOTOR_ROTATOR_ITEM = ITEMS.registerSimpleBlockItem("motor_rotator", MOTOR_ROTATOR);
    public static final DeferredItem<BlockItem> REVERSED_MOTOR_ROTATOR_ITEM = ITEMS.registerSimpleBlockItem("reversed_motor_rotator", REVERSED_MOTOR_ROTATOR);
    public static final DeferredItem<BlockItem> WINDSAIL_WORKER_ITEM = ITEMS.registerSimpleBlockItem("windsail_worker", WINDSAIL_WORKER);
    public static final DeferredItem<BlockItem> INVERTED_WINDSAIL_WORKER_ITEM = ITEMS.registerSimpleBlockItem("inverted_windsail_worker", INVERTED_WINDSAIL_WORKER);
    public static final DeferredItem<BlockItem> WINDSAIL_ITEM = ITEMS.registerSimpleBlockItem("windsail", WINDSAIL);

    public static final DeferredHolder<EntityType<?>, EntityType<AerovoxVehicle>> VEHICLE = ENTITY_TYPES.register(
            "aircraft",
            () -> EntityType.Builder.of(AerovoxVehicle::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(AerovoxMod.MODID + ":aircraft"));

    public static final DeferredHolder<EntityType<?>, EntityType<BombEntity>> BOMB = ENTITY_TYPES.register(
            "bomb",
            () -> EntityType.Builder.of(BombEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(AerovoxMod.MODID + ":bomb"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aerovox.main"))
                    .icon(() -> SUPERGLUE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(SUPERGLUE.get());
                        output.accept(ASSEMBLY_TOOL.get());
                        output.accept(CONTROLLER_REMOTE.get());
                        output.accept(ENGINE_ITEM.get());
                        output.accept(MOTOR_ITEM.get());
                        output.accept(MOTOR_ROTATOR_ITEM.get());
                        output.accept(REVERSED_MOTOR_ROTATOR_ITEM.get());
                        output.accept(WINDSAIL_WORKER_ITEM.get());
                        output.accept(INVERTED_WINDSAIL_WORKER_ITEM.get());
                        output.accept(CONTROL_SURFACE_ITEM.get());
                        output.accept(WINDSAIL_ITEM.get());
                        output.accept(PILOT_SEAT_ITEM.get());
                        output.accept(CONTROLLER_ITEM.get());
                        output.accept(SMALL_BOMB_MOUNT_ITEM.get());
                        output.accept(BOMB_MOUNT_ITEM.get());
                        output.accept(BIG_BOMB_MOUNT_ITEM.get());
                        output.accept(LANDING_GEAR_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITY_TYPES.register(bus);
        TABS.register(bus);
    }

    private AerovoxRegistries() {}
}
