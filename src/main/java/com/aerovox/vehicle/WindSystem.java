package com.aerovox.vehicle;

import com.aerovox.AerovoxConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Wind in blocks per tick. Derived from game time, so client and server agree without syncing. */
public final class WindSystem {
    private WindSystem() {}

    public static Vec3 sample(Level level) {
        if (!AerovoxConfig.WIND_ENABLED.get()) return Vec3.ZERO;
        double seconds = level.getGameTime() / 20.0;
        double baseSpeed = AerovoxConfig.WIND_SPEED.get() / 20.0;
        double direction = Math.toRadians(45.0);
        double gust = 1.0;
        if (AerovoxConfig.WIND_VARIATION.get()) {
            direction += 0.5 * Math.sin(seconds * 0.02);
            gust += 0.25 * Math.sin(seconds * 0.13) + 0.1 * Math.sin(seconds * 0.41);
        }
        double speed = baseSpeed * gust;
        return new Vec3(Math.cos(direction) * speed, 0.0, Math.sin(direction) * speed);
    }
}
