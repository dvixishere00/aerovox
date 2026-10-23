package com.aerovox.vehicle;

import com.aerovox.AerovoxConfig;
import com.aerovox.AerovoxRegistries;
import com.aerovox.assembly.AssemblyToolItem;
import com.aerovox.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An assembled structure flown by the flight model. The entity position is the center of mass.
 * Block offsets are relative to it, in the aircraft's local frame.
 * Piloting needs a pilot seat and a controller block in the structure.
 */
public class AerovoxVehicle extends Entity {

    /** A block center relative to the center of mass, plus its block entity data if it had one. */
    public record VehicleBlock(double cx, double cy, double cz, BlockState state, CompoundTag blockEntity) {}

    /** Key names, in bit order. Bit i of the control mask is key i. */
    public static final String[] KEY_NAMES = {"W", "S", "A", "D", "Space", "Ctrl"};
    public static final int KEY_COUNT = 6;

    private static final double THROTTLE_RATE = 0.01;
    private static final double MAX_SUBSTEP = 0.25;
    private static final int[] AXIS_ORDER = {1, 0, 2};
    private static final double FRICTION_WITH_GEAR = 0.98;
    private static final double FRICTION_WITHOUT_GEAR = 0.9;
    private static final double WORKER_SPEED_DEG = 3.0;
    private static final double WORKER_OPEN_DEG = 45.0;

    private static final EntityDataAccessor<CompoundTag> DATA_STRUCTURE =
            SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> DATA_QX = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_QY = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_QZ = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_QW = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THROTTLE = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROTOR = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WORKER = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_STALLED = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BINDINGS = SynchedEntityData.defineId(AerovoxVehicle.class, EntityDataSerializers.INT);

    private List<VehicleBlock> blocks = List.of();
    private List<BladeLayout.Blade> layout = List.of();
    private FlightModel model;
    private final FlightPhysics.State flight = new FlightPhysics.State();
    private double throttle;
    private int controlMask;
    private boolean bombHeld;
    private boolean motorsHeld;
    private boolean workersHeld;
    private boolean motorsOn;
    private boolean workersOpen;
    private double rotorAngle;
    private double workerAngle;
    private int bombIndex;
    private boolean disassembling;
    private UUID owner;
    private double cullRadius = 1.0;

    public AerovoxVehicle(EntityType<? extends AerovoxVehicle> type, Level level) {
        super(type, level);
        this.setNoGravity(true); // gravity is applied by the flight model
    }

    // ---------------- Structure ----------------

    public void setStructure(List<VehicleBlock> newBlocks) {
        this.blocks = List.copyOf(newBlocks);
        this.model = blocks.isEmpty() ? null : FlightModel.build(blocks);
        this.layout = BladeLayout.compute(blocks);
        this.cullRadius = radiusOf(this.blocks);
        this.entityData.set(DATA_STRUCTURE, encode(this.blocks));
    }

    public List<VehicleBlock> getBlocks() {
        ensureClientStructure();
        return blocks;
    }

    public List<BladeLayout.Blade> getBladeLayout() {
        ensureClientStructure();
        return layout;
    }

    private void ensureClientStructure() {
        if (blocks.isEmpty() && level().isClientSide()) {
            CompoundTag data = entityData.get(DATA_STRUCTURE);
            if (!data.isEmpty()) {
                blocks = decode(data, level().registryAccess().lookupOrThrow(Registries.BLOCK));
                layout = BladeLayout.compute(blocks);
                cullRadius = radiusOf(blocks);
            }
        }
    }

    public boolean hasController() {
        return model != null && model.hasController;
    }

    public void setInitialYaw(float yawDegrees) {
        // Heading convention: forward = (-sin(yaw), cos(yaw)), so rotate by -yaw about +Y.
        flight.orientation.rotationY((float) Math.toRadians(-yawDegrees));
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setControlMask(int mask) {
        this.controlMask = mask;
    }

    public int getBindings() {
        return entityData.get(DATA_BINDINGS);
    }

    public void setBindings(int packed) {
        entityData.set(DATA_BINDINGS, packed);
    }

    // ---------------- Client-facing state ----------------

    public Quaternionf getOrientation() {
        if (!level().isClientSide()) return flight.orientation;
        return new Quaternionf(entityData.get(DATA_QX), entityData.get(DATA_QY),
                entityData.get(DATA_QZ), entityData.get(DATA_QW));
    }

    /** {heading, pitch, roll} in degrees. */
    public double[] attitude() {
        return FlightPhysics.attitude(getOrientation());
    }

    public float getThrottle() {
        return entityData.get(DATA_THROTTLE);
    }

    public float getRotorAngle() {
        return entityData.get(DATA_ROTOR);
    }

    public float getWorkerAngle() {
        return entityData.get(DATA_WORKER);
    }

    public boolean isStalled() {
        return entityData.get(DATA_STALLED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STRUCTURE, new CompoundTag());
        builder.define(DATA_QX, 0.0f);
        builder.define(DATA_QY, 0.0f);
        builder.define(DATA_QZ, 0.0f);
        builder.define(DATA_QW, 1.0f);
        builder.define(DATA_THROTTLE, 0.0f);
        builder.define(DATA_ROTOR, 0.0f);
        builder.define(DATA_WORKER, 0.0f);
        builder.define(DATA_STALLED, false);
        builder.define(DATA_BINDINGS, ControlAction.DEFAULT_BINDINGS);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_STRUCTURE.equals(key)) {
            blocks = List.of();
            ensureClientStructure();
        }
    }

    // ---------------- Simulation ----------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || model == null) return;

        boolean piloted = getControllingPassenger() instanceof Player;
        int pressed = piloted ? controlMask : 0;
        int bindings = getBindings();

        boolean pitchUp = false, pitchDown = false, rollLeft = false, rollRight = false;
        boolean throttleUp = false, throttleDown = false;
        boolean bomb = false, motorToggle = false, workerToggle = false;
        for (int key = 0; key < KEY_COUNT; key++) {
            if ((pressed & (1 << key)) == 0) continue;
            switch (ControlAction.of(bindings, key)) {
                case PITCH_UP -> pitchUp = true;
                case PITCH_DOWN -> pitchDown = true;
                case ROLL_LEFT -> rollLeft = true;
                case ROLL_RIGHT -> rollRight = true;
                case THROTTLE_UP -> throttleUp = true;
                case THROTTLE_DOWN -> throttleDown = true;
                case BOMB_RELEASE -> bomb = true;
                case TOGGLE_MOTORS -> motorToggle = true;
                case TOGGLE_WORKERS -> workerToggle = true;
                default -> { }
            }
        }

        double pitchCmd = (pitchUp ? 1 : 0) - (pitchDown ? 1 : 0);
        double rollCmd = (rollRight ? 1 : 0) - (rollLeft ? 1 : 0);
        if (throttleUp) throttle += THROTTLE_RATE;
        if (throttleDown) throttle -= THROTTLE_RATE;
        throttle = Mth.clamp(throttle, 0.0, 1.0);

        // Toggles and bomb releases act once per key press, not every tick the key is held.
        if (motorToggle && !motorsHeld) motorsOn = !motorsOn;
        motorsHeld = motorToggle;
        if (workerToggle && !workersHeld) workersOpen = !workersOpen;
        workersHeld = workerToggle;
        if (bomb && !bombHeld) releaseBomb();
        bombHeld = bomb;

        boolean rotorsActive = motorsOn && model.motorCount > 0;
        if (rotorsActive) {
            rotorAngle = (rotorAngle + 0.2 + 0.8 * throttle) % (2.0 * Math.PI);
        }
        double target = workersOpen ? WORKER_OPEN_DEG : 0.0;
        workerAngle += Mth.clamp(target - workerAngle, -WORKER_SPEED_DEG, WORKER_SPEED_DEG);

        Vec3 wind = WindSystem.sample(level());
        FlightPhysics.step(model, flight, wind, throttle, pitchCmd, rollCmd, rotorsActive);
        moveWithCollision(model.landingGearCount > 0 ? FRICTION_WITH_GEAR : FRICTION_WITHOUT_GEAR);
        syncToClients();

        for (Entity passenger : getPassengers()) {
            positionRider(passenger, Entity::setPos);
        }
    }

    private void releaseBomb() {
        if (model.bombMounts.isEmpty()) return;
        FlightModel.BombMount mount = model.bombMounts.get(bombIndex % model.bombMounts.size());
        bombIndex++;
        Vec3 pos = mount.pos();
        Vector3f r = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
        flight.orientation.transform(r);
        BombEntity bomb = AerovoxRegistries.BOMB.get().create(level());
        if (bomb == null) return;
        bomb.setPower(mount.size().power());
        bomb.setPos(getX() + r.x, getY() + r.y - 0.5, getZ() + r.z);
        bomb.setDeltaMovement(flight.velocity);
        level().addFreshEntity(bomb);
    }

    /** Moves along each axis in small steps, stopping at the first collision with the world. */
    private void moveWithCollision(double groundFriction) {
        Vec3 v = flight.velocity;
        double[] pos = {getX(), getY(), getZ()};
        double[] vel = {v.x, v.y, v.z};
        boolean landed = false;
        Quaternionf q = flight.orientation;

        for (int axis : AXIS_ORDER) {
            double amount = vel[axis];
            if (amount == 0.0) continue;
            int steps = Math.max(1, (int) Math.ceil(Math.abs(amount) / MAX_SUBSTEP));
            double step = amount / steps;
            boolean blocked = false;
            for (int i = 0; i < steps; i++) {
                double[] candidate = pos.clone();
                candidate[axis] += step;
                if (collides(candidate[0], candidate[1], candidate[2], q)) {
                    blocked = true;
                    break;
                }
                pos = candidate;
            }
            if (blocked) {
                if (axis == 1 && amount < 0) landed = true;
                vel[axis] = 0.0;
            }
        }

        if (landed) {
            vel[0] *= groundFriction;
            vel[2] *= groundFriction;
        }

        double minY = level().getMinBuildHeight();
        if (pos[1] < minY) {
            pos[1] = minY;
            vel[1] = 0.0;
        }

        setPos(pos[0], pos[1], pos[2]);
        flight.velocity = new Vec3(vel[0], vel[1], vel[2]);
        setDeltaMovement(flight.velocity);
        setOnGround(landed);
    }

    /** True if any structure block, placed around (px, py, pz) with orientation q, overlaps world collision. */
    private boolean collides(double px, double py, double pz, Quaternionf q) {
        Matrix3f m = q.get(new Matrix3f());
        double hx = 0.5 * (Math.abs(m.m00) + Math.abs(m.m10) + Math.abs(m.m20));
        double hy = 0.5 * (Math.abs(m.m01) + Math.abs(m.m11) + Math.abs(m.m21));
        double hz = 0.5 * (Math.abs(m.m02) + Math.abs(m.m12) + Math.abs(m.m22));

        for (VehicleBlock b : blocks) {
            Vector3f r = new Vector3f((float) b.cx(), (float) b.cy(), (float) b.cz());
            q.transform(r);
            double wx = px + r.x, wy = py + r.y, wz = pz + r.z;
            AABB box = new AABB(wx - hx, wy - hy, wz - hz, wx + hx, wy + hy, wz + hz);
            if (!level().noCollision(box)) return true;
        }
        return false;
    }

    private void syncToClients() {
        Quaternionf q = flight.orientation;
        entityData.set(DATA_QX, q.x);
        entityData.set(DATA_QY, q.y);
        entityData.set(DATA_QZ, q.z);
        entityData.set(DATA_QW, q.w);
        entityData.set(DATA_THROTTLE, (float) throttle);
        entityData.set(DATA_ROTOR, (float) rotorAngle);
        entityData.set(DATA_WORKER, (float) workerAngle);
        entityData.set(DATA_STALLED, flight.stalled);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(cullRadius);
    }

    // ---------------- Seating and control ----------------

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (!(passenger instanceof Player player)) return false;
        if (!getPassengers().isEmpty() || model == null || !model.hasPilotSeat || !model.hasController) return false;
        if (AerovoxConfig.PILOT_RESTRICT_TO_OWNER.get() && owner != null && !owner.equals(player.getUUID())) return false;
        return true;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (model == null || !model.hasPilotSeat) {
            super.positionRider(passenger, callback);
            return;
        }
        Vec3 seat = model.seatPosition;
        Vector3f r = new Vector3f((float) seat.x, (float) (seat.y + 0.5), (float) seat.z);
        flight.orientation.transform(r);
        callback.accept(passenger, getX() + r.x, getY() + r.y, getZ() + r.z);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (held.is(AerovoxRegistries.CONTROLLER_REMOTE.get())) {
                if (level().isClientSide()) ClientHooks.openBindings(getId(), getBindings());
                return InteractionResult.SUCCESS;
            }
            if (held.getItem() instanceof AssemblyToolItem) {
                if (level().isClientSide()) return InteractionResult.SUCCESS;
                if (level() instanceof ServerLevel serverLevel) return disassemble(serverLevel, player);
            }
            return InteractionResult.PASS;
        }
        if (canAddPassenger(player)) {
            if (!level().isClientSide()) player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---------------- Disassembly and kill safety ----------------

    private InteractionResult disassemble(ServerLevel level, Player player) {
        if (!placeStructure(level, player, true)) return InteractionResult.FAIL;
        disassembling = true;
        discard();
        player.displayClientMessage(Component.literal("Disassembled aircraft."), false);
        return InteractionResult.SUCCESS;
    }

    /** If the aircraft is killed or discarded while assembled, its blocks go back into the world. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && !disassembling && model != null
                && (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
                && level() instanceof ServerLevel serverLevel) {
            placeStructure(serverLevel, null, false);
        }
        super.remove(reason);
    }

    /**
     * Places the structure at the aircraft's current position.
     * With requireAligned, the aircraft must be level and facing a cardinal direction, and any blocked
     * space fails the whole operation. Without it, blocked blocks drop as items so nothing is lost.
     */
    private boolean placeStructure(ServerLevel level, Player player, boolean requireAligned) {
        double[] att = attitude();
        int quarter = Math.floorMod((int) Math.round(att[0] / 90.0), 4);
        if (requireAligned) {
            double headingError = wrapDegrees(att[0] - quarter * 90.0);
            if (Math.abs(att[1]) > 5 || Math.abs(att[2]) > 5 || Math.abs(headingError) > 5) {
                fail(player, "Land level and face N, E, S, or W before disassembling.");
                return false;
            }
        }

        Rotation rotation = rotationFor(quarter);
        List<BlockPos> targets = new ArrayList<>(blocks.size());
        List<BlockState> states = new ArrayList<>(blocks.size());
        List<CompoundTag> tiles = new ArrayList<>(blocks.size());
        for (VehicleBlock b : blocks) {
            double[] r = rotateQuarter(b.cx(), b.cz(), quarter);
            BlockPos target = new BlockPos(
                    (int) Math.round(getX() + r[0] - 0.5),
                    (int) Math.round(getY() + b.cy() - 0.5),
                    (int) Math.round(getZ() + r[1] - 0.5));
            if (requireAligned) {
                BlockState current = level.getBlockState(target);
                if (!(current.isAir() || current.canBeReplaced())) {
                    fail(player, "Cannot disassemble: space is blocked at " + target.toShortString() + ".");
                    return false;
                }
            }
            targets.add(target);
            states.add(b.state().rotate(rotation));
            tiles.add(b.blockEntity());
        }

        ejectPassengers();
        for (int i = 0; i < targets.size(); i++) {
            BlockPos target = targets.get(i);
            BlockState current = level.getBlockState(target);
            if (!(current.isAir() || current.canBeReplaced())) {
                Block.popResource(level, target, new ItemStack(states.get(i).getBlock()));
                continue;
            }
            level.setBlock(target, states.get(i), Block.UPDATE_ALL);
            if (tiles.get(i) != null) {
                BlockEntity be = level.getBlockEntity(target);
                if (be != null) be.loadWithComponents(tiles.get(i), level.registryAccess());
            }
        }
        return true;
    }

    private static double[] rotateQuarter(double x, double z, int quarter) {
        double rx = x, rz = z;
        for (int i = 0; i < quarter; i++) {
            double nx = -rz;
            double nz = rx;
            rx = nx;
            rz = nz;
        }
        return new double[]{rx, rz};
    }

    private static Rotation rotationFor(int quarter) {
        return switch (quarter) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static double wrapDegrees(double d) {
        return ((d % 360.0) + 540.0) % 360.0 - 180.0;
    }

    private static void fail(Player player, String message) {
        if (player != null) player.displayClientMessage(Component.literal(message), false);
    }

    // ---------------- Misc entity behavior ----------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---------------- Persistence ----------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStructure(decode(tag.getCompound("structure"), level().registryAccess().lookupOrThrow(Registries.BLOCK)));
        if (tag.contains("qw")) {
            flight.orientation.set(tag.getFloat("qx"), tag.getFloat("qy"), tag.getFloat("qz"), tag.getFloat("qw"));
        }
        flight.velocity = new Vec3(tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz"));
        flight.angularVelocity = new Vec3(tag.getDouble("wx"), tag.getDouble("wy"), tag.getDouble("wz"));
        throttle = tag.getFloat("throttle");
        rotorAngle = tag.getDouble("rotor");
        workerAngle = tag.getDouble("worker");
        motorsOn = tag.getBoolean("motorsOn");
        workersOpen = tag.getBoolean("workersOpen");
        bombIndex = tag.getInt("bombIndex");
        if (tag.contains("bindings")) setBindings(tag.getInt("bindings"));
        if (tag.hasUUID("owner")) owner = tag.getUUID("owner");
        setDeltaMovement(flight.velocity);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("structure", encode(blocks));
        Quaternionf q = flight.orientation;
        tag.putFloat("qx", q.x);
        tag.putFloat("qy", q.y);
        tag.putFloat("qz", q.z);
        tag.putFloat("qw", q.w);
        tag.putDouble("vx", flight.velocity.x);
        tag.putDouble("vy", flight.velocity.y);
        tag.putDouble("vz", flight.velocity.z);
        tag.putDouble("wx", flight.angularVelocity.x);
        tag.putDouble("wy", flight.angularVelocity.y);
        tag.putDouble("wz", flight.angularVelocity.z);
        tag.putFloat("throttle", (float) throttle);
        tag.putDouble("rotor", rotorAngle);
        tag.putDouble("worker", workerAngle);
        tag.putBoolean("motorsOn", motorsOn);
        tag.putBoolean("workersOpen", workersOpen);
        tag.putInt("bombIndex", bombIndex);
        tag.putInt("bindings", getBindings());
        if (owner != null) tag.putUUID("owner", owner);
    }

    static CompoundTag encode(List<VehicleBlock> blocks) {
        ListTag list = new ListTag();
        for (VehicleBlock b : blocks) {
            CompoundTag t = new CompoundTag();
            t.putDouble("x", b.cx());
            t.putDouble("y", b.cy());
            t.putDouble("z", b.cz());
            t.put("state", NbtUtils.writeBlockState(b.state()));
            if (b.blockEntity() != null) t.put("tile", b.blockEntity());
            list.add(t);
        }
        CompoundTag root = new CompoundTag();
        root.put("blocks", list);
        return root;
    }

    static List<VehicleBlock> decode(CompoundTag root, HolderGetter<Block> blockLookup) {
        List<VehicleBlock> out = new ArrayList<>();
        ListTag list = root.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new VehicleBlock(
                    t.getDouble("x"), t.getDouble("y"), t.getDouble("z"),
                    NbtUtils.readBlockState(blockLookup, t.getCompound("state")),
                    t.contains("tile") ? t.getCompound("tile") : null));
        }
        return out;
    }

    private static double radiusOf(List<VehicleBlock> blocks) {
        double max = 1.0;
        for (VehicleBlock b : blocks) {
            max = Math.max(max, Math.max(Math.abs(b.cx()), Math.max(Math.abs(b.cy()), Math.abs(b.cz()))) + 1.0);
        }
        return max;
    }
}
