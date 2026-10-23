package com.aerovox.vehicle;

import com.aerovox.AerovoxRegistries;
import com.aerovox.vehicle.AerovoxVehicle.VehicleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Which blocks spin with a rotator or rotate with a worker. A blade is any non-head block
 * up to 2 blocks sideways and vertically from a head, and 1 to 2 blocks in front of it.
 * Blades are only drawn moving. Physics ignores them, so they don't change mass or collisions.
 * Client and server compute the same layout from the same block list.
 */
public final class BladeLayout {
    public enum Kind { ROTOR, REVERSED_ROTOR, WORKER, INVERTED_WORKER }

    public record Blade(int block, double px, double py, double pz, Kind kind) {}

    private BladeLayout() {}

    public static Kind kindOf(BlockState s) {
        Block b = s.getBlock();
        if (b == AerovoxRegistries.MOTOR_ROTATOR.get()) return Kind.ROTOR;
        if (b == AerovoxRegistries.REVERSED_MOTOR_ROTATOR.get()) return Kind.REVERSED_ROTOR;
        if (b == AerovoxRegistries.WINDSAIL_WORKER.get()) return Kind.WORKER;
        if (b == AerovoxRegistries.INVERTED_WINDSAIL_WORKER.get()) return Kind.INVERTED_WORKER;
        return null;
    }

    public static List<Blade> compute(List<VehicleBlock> blocks) {
        List<Integer> heads = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (kindOf(blocks.get(i).state()) != null) heads.add(i);
        }
        List<Blade> out = new ArrayList<>();
        if (heads.isEmpty()) return out;

        for (int i = 0; i < blocks.size(); i++) {
            VehicleBlock b = blocks.get(i);
            if (kindOf(b.state()) != null) continue;
            for (int h : heads) {
                VehicleBlock head = blocks.get(h);
                double dx = b.cx() - head.cx();
                double dy = b.cy() - head.cy();
                double dz = b.cz() - head.cz();
                if (Math.abs(dx) <= 2.01 && Math.abs(dy) <= 2.01 && dz >= 0.99 && dz <= 2.01) {
                    out.add(new Blade(i, head.cx(), head.cy(), head.cz(), kindOf(head.state())));
                    break;
                }
            }
        }
        return out;
    }
}
