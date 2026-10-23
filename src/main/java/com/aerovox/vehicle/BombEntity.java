package com.aerovox.vehicle;

import com.aerovox.AerovoxConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A released bomb. It falls as an entity under gravity and explodes on impact. */
public class BombEntity extends Entity {
    private static final double GRAVITY = 0.04;
    private static final int MAX_LIFETIME_TICKS = 1200;
    private float power = 3.0f;
    private boolean exploded;

    public BombEntity(EntityType<? extends BombEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public void setPower(float power) {
        this.power = power;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || exploded) return;

        Vec3 v = getDeltaMovement().add(0.0, -GRAVITY, 0.0);
        setDeltaMovement(v);
        move(MoverType.SELF, v);

        if (onGround() || horizontalCollision) {
            explode();
        } else if (tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    private void explode() {
        exploded = true;
        if (level() instanceof ServerLevel serverLevel) {
            Level.ExplosionInteraction mode = AerovoxConfig.BOMB_BREAK_BLOCKS.get()
                    ? Level.ExplosionInteraction.BLOCK
                    : Level.ExplosionInteraction.NONE;
            serverLevel.explode(this, getX(), getY(), getZ(), power, mode);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
