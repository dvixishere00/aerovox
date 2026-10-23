package com.aerovox.assembly;

import com.aerovox.AerovoxConfig;
import com.aerovox.AerovoxRegistries;
import com.aerovox.glue.GlueData;
import com.aerovox.vehicle.AerovoxVehicle;
import com.aerovox.vehicle.AerovoxVehicle.VehicleBlock;
import com.aerovox.vehicle.FlightModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Converts a superglued group into an aircraft. The center of mass becomes the entity position.
 * Blocks leave the world only after the entity exists, so a failure cannot lose blocks.
 * Block entity data (chest contents, sign text) is captured and restored on disassembly.
 */
public final class AircraftAssembler {
    private AircraftAssembler() {}

    public static InteractionResult assemble(ServerLevel level, Player player, GlueData glue, UUID groupId) {
        Set<BlockPos> positions = glue.positionsOf(groupId);

        List<BlockPos> solid = new ArrayList<>();
        for (BlockPos p : positions) {
            if (!level.getBlockState(p).isAir()) solid.add(p);
        }
        if (solid.isEmpty()) {
            glue.removeGroup(groupId);
            return fail(player, "That structure is empty; the glue was removed.");
        }
        if (solid.size() > AerovoxConfig.MAX_VEHICLE_BLOCKS.get()) {
            return fail(player, "Structure has " + solid.size() + " blocks; assembly limit is "
                    + AerovoxConfig.MAX_VEHICLE_BLOCKS.get() + ".");
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : solid) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxZ = Math.max(maxZ, p.getZ());
        }

        // Reference point: bottom center of the footprint. Raw centers are measured from it.
        BlockPos ref = new BlockPos(minX + (maxX - minX) / 2, minY, minZ + (maxZ - minZ) / 2);
        List<VehicleBlock> raw = new ArrayList<>(solid.size());
        for (BlockPos p : solid) {
            BlockEntity be = level.getBlockEntity(p);
            CompoundTag tile = be == null ? null : be.saveWithFullMetadata(level.registryAccess());
            raw.add(new VehicleBlock(
                    p.getX() + 0.5 - ref.getX(),
                    p.getY() + 0.5 - ref.getY(),
                    p.getZ() + 0.5 - ref.getZ(),
                    level.getBlockState(p),
                    tile));
        }

        FlightModel model = FlightModel.build(raw);
        Vec3 com = model.com;
        List<VehicleBlock> local = new ArrayList<>(raw.size());
        for (VehicleBlock b : raw) {
            local.add(new VehicleBlock(b.cx() - com.x, b.cy() - com.y, b.cz() - com.z, b.state(), b.blockEntity()));
        }

        AerovoxVehicle vehicle = AerovoxRegistries.VEHICLE.get().create(level);
        if (vehicle == null) {
            return fail(player, "Could not create the aircraft entity.");
        }

        // Commit: leave the world, release glue, spawn the aircraft.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos p : solid) {
            level.setBlock(p, air, Block.UPDATE_ALL);
        }
        glue.removeGroup(groupId);

        vehicle.setPos(ref.getX() + com.x, ref.getY() + com.y, ref.getZ() + com.z);
        vehicle.setInitialYaw(Math.round(player.getYRot() / 90f) * 90f);
        vehicle.setOwner(player.getUUID());
        vehicle.setStructure(local);
        level.addFreshEntity(vehicle);

        player.displayClientMessage(Component.literal("Assembled aircraft with " + local.size() + " blocks."), false);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult fail(Player player, String message) {
        player.displayClientMessage(Component.literal(message), false);
        return InteractionResult.FAIL;
    }
}
