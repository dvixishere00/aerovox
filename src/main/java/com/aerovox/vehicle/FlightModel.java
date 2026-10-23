package com.aerovox.vehicle;

import com.aerovox.AerovoxConfig;
import com.aerovox.AerovoxRegistries;
import com.aerovox.AerovoxTags;
import com.aerovox.vehicle.AerovoxVehicle.VehicleBlock;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Mass, center of mass, inertia, lift, drag, sails, rotors, and control properties derived from the structure.
 * Positions are relative to the center of mass in the local frame (+Z forward, +Y up, -X right wing).
 */
public final class FlightModel {
    public enum Role { ELEVATOR, AILERON, NONE }

    public enum BombSize {
        SMALL, NORMAL, BIG;

        public float power() {
            return switch (this) {
                case SMALL -> AerovoxConfig.BOMB_POWER_SMALL.get().floatValue();
                case NORMAL -> AerovoxConfig.BOMB_POWER.get().floatValue();
                case BIG -> AerovoxConfig.BOMB_POWER_BIG.get().floatValue();
            };
        }
    }

    public record ControlSurface(Vec3 pos, Role role) {}
    public record Sail(Vec3 pos, Vec3 normal) {}
    /** A motor rotator. direction is +1 for normal thrust, -1 for reversed. */
    public record Rotor(Vec3 pos, double direction) {}
    public record BombMount(Vec3 pos, BombSize size) {}

    public double mass = 1.0;
    public Vec3 com = Vec3.ZERO;
    public double[] inertia = {1, 1, 1};
    public Vec3 centerOfLift = Vec3.ZERO;
    public double liftArea;
    public double sizeX = 1, sizeY = 1, sizeZ = 1;
    public double finArea;
    public Vec3 finPosition = Vec3.ZERO;
    public List<Vec3> engines = List.of();
    public List<Rotor> rotors = List.of();
    public int motorCount;
    public List<ControlSurface> controlSurfaces = List.of();
    public List<Sail> sails = List.of();
    public List<BombMount> bombMounts = List.of();
    public int landingGearCount;
    public boolean hasPilotSeat;
    public boolean hasController;
    public Vec3 seatPosition = Vec3.ZERO;

    private FlightModel() {}

    /** Lightweight materials weigh less; engines weigh more. Values are tuning constants. */
    public static double massOf(BlockState s) {
        if (s.is(AerovoxTags.ENGINES)) return 2.0;
        if (s.is(BlockTags.WOOL)) return 0.5;
        if (s.is(BlockTags.PLANKS) || s.is(BlockTags.LOGS)) return 0.8;
        if (s.getBlock() instanceof TransparentBlock) return 0.6;
        return 1.0;
    }

    public static FlightModel build(List<VehicleBlock> blocks) {
        FlightModel m = new FlightModel();
        if (blocks.isEmpty()) return m;

        double totalMass = 0, mx = 0, my = 0, mz = 0;
        for (VehicleBlock b : blocks) {
            double w = massOf(b.state());
            totalMass += w;
            mx += w * b.cx();
            my += w * b.cy();
            mz += w * b.cz();
        }
        if (totalMass <= 0) totalMass = 1;
        m.mass = totalMass;
        m.com = new Vec3(mx / totalMass, my / totalMass, mz / totalMass);

        double minRx = Double.MAX_VALUE, maxRx = -Double.MAX_VALUE;
        double minRy = Double.MAX_VALUE, maxRy = -Double.MAX_VALUE;
        double minRz = Double.MAX_VALUE, maxRz = -Double.MAX_VALUE;
        for (VehicleBlock b : blocks) {
            double rx = b.cx() - m.com.x, ry = b.cy() - m.com.y, rz = b.cz() - m.com.z;
            minRx = Math.min(minRx, rx); maxRx = Math.max(maxRx, rx);
            minRy = Math.min(minRy, ry); maxRy = Math.max(maxRy, ry);
            minRz = Math.min(minRz, rz); maxRz = Math.max(maxRz, rz);
        }
        m.sizeX = maxRx - minRx + 1;
        m.sizeY = maxRy - minRy + 1;
        m.sizeZ = maxRz - minRz + 1;
        double rearLimit = minRz + m.sizeZ / 3.0;

        double[] inertia = {totalMass / 6.0, totalMass / 6.0, totalMass / 6.0};
        double liftArea = 0, lx = 0, ly = 0, lz = 0;
        double finArea = 0, fx = 0, fy = 0, fz = 0;
        List<Vec3> engines = new ArrayList<>();
        List<Rotor> rotors = new ArrayList<>();
        int motors = 0;
        List<ControlSurface> surfaces = new ArrayList<>();
        List<Sail> sails = new ArrayList<>();
        List<BombMount> mounts = new ArrayList<>();
        boolean hasSeat = false, hasController = false;
        Vec3 seat = Vec3.ZERO;
        int gear = 0;

        for (VehicleBlock b : blocks) {
            BlockState st = b.state();
            double w = massOf(st);
            double rx = b.cx() - m.com.x, ry = b.cy() - m.com.y, rz = b.cz() - m.com.z;
            Vec3 r = new Vec3(rx, ry, rz);

            inertia[0] += w * (ry * ry + rz * rz);
            inertia[1] += w * (rx * rx + rz * rz);
            inertia[2] += w * (rx * rx + ry * ry);

            if (st.is(AerovoxTags.LIFTING_SURFACES)) {
                liftArea += 1; lx += rx; ly += ry; lz += rz;
            }
            if (rz <= rearLimit) {
                finArea += 1; fx += rx; fy += ry; fz += rz;
            }
            if (st.is(AerovoxTags.ENGINES)) engines.add(r);
            if (st.is(AerovoxTags.LANDING_GEAR)) gear++;
            if (st.is(AerovoxTags.CONTROL_SURFACES)) {
                Role role = Math.abs(rx) >= 1.0 ? Role.AILERON : (rz < 0 ? Role.ELEVATOR : Role.NONE);
                surfaces.add(new ControlSurface(r, role));
            }
            if (st.getBlock() == AerovoxRegistries.WINDSAIL.get()) {
                Direction facing = st.getValue(HorizontalDirectionalBlock.FACING);
                sails.add(new Sail(r, new Vec3(facing.getStepX(), 0, facing.getStepZ())));
            }
            if (st.getBlock() == AerovoxRegistries.MOTOR.get()) motors++;
            if (st.getBlock() == AerovoxRegistries.MOTOR_ROTATOR.get()) rotors.add(new Rotor(r, 1.0));
            if (st.getBlock() == AerovoxRegistries.REVERSED_MOTOR_ROTATOR.get()) rotors.add(new Rotor(r, -1.0));
            if (st.getBlock() == AerovoxRegistries.SMALL_BOMB_MOUNT.get()) mounts.add(new BombMount(r, BombSize.SMALL));
            if (st.getBlock() == AerovoxRegistries.BOMB_MOUNT.get()) mounts.add(new BombMount(r, BombSize.NORMAL));
            if (st.getBlock() == AerovoxRegistries.BIG_BOMB_MOUNT.get()) mounts.add(new BombMount(r, BombSize.BIG));
            if (st.getBlock() == AerovoxRegistries.CONTROLLER.get()) hasController = true;
            if (!hasSeat && st.getBlock() == AerovoxRegistries.PILOT_SEAT.get()) {
                hasSeat = true;
                seat = r;
            }
        }

        m.inertia = inertia;
        m.liftArea = liftArea;
        m.centerOfLift = liftArea > 0 ? new Vec3(lx / liftArea, ly / liftArea, lz / liftArea) : Vec3.ZERO;
        m.finArea = finArea;
        m.finPosition = finArea > 0 ? new Vec3(fx / finArea, fy / finArea, fz / finArea) : Vec3.ZERO;
        m.engines = engines;
        m.rotors = rotors;
        m.motorCount = motors;
        m.controlSurfaces = surfaces;
        m.sails = sails;
        m.bombMounts = mounts;
        m.landingGearCount = gear;
        m.hasPilotSeat = hasSeat;
        m.hasController = hasController;
        m.seatPosition = seat;
        return m;
    }
}
