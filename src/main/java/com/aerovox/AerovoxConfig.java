package com.aerovox;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AerovoxConfig {
    public static final ModConfigSpec.IntValue MAX_GLUE_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_VEHICLE_BLOCKS;
    public static final ModConfigSpec.BooleanValue WIND_ENABLED;
    public static final ModConfigSpec.DoubleValue WIND_SPEED;
    public static final ModConfigSpec.BooleanValue WIND_VARIATION;
    public static final ModConfigSpec.BooleanValue PILOT_RESTRICT_TO_OWNER;
    public static final ModConfigSpec.DoubleValue BOMB_POWER_SMALL;
    public static final ModConfigSpec.DoubleValue BOMB_POWER;
    public static final ModConfigSpec.DoubleValue BOMB_POWER_BIG;
    public static final ModConfigSpec.BooleanValue BOMB_BREAK_BLOCKS;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("superglue");
        MAX_GLUE_BLOCKS = b.comment("Maximum blocks in one superglue selection.")
                .defineInRange("maxBlocks", 4096, 1, 262144);
        b.pop();
        b.push("assembly");
        MAX_VEHICLE_BLOCKS = b.comment("Maximum blocks in one aircraft. Higher values cost more CPU per tick.")
                .defineInRange("maxBlocks", 1024, 1, 65536);
        b.pop();
        b.push("wind");
        WIND_ENABLED = b.comment("Enable wind. Disable for calm air.").define("enabled", true);
        WIND_SPEED = b.comment("Base wind speed in blocks per second.")
                .defineInRange("speedBlocksPerSecond", 4.0, 0.0, 40.0);
        WIND_VARIATION = b.comment("Let wind direction and gusts vary over time.").define("variation", true);
        b.pop();
        b.push("pilot");
        PILOT_RESTRICT_TO_OWNER = b.comment("Only the player who assembled an aircraft may sit in its pilot seat.")
                .define("restrictToOwner", false);
        b.pop();
        b.push("bombs");
        BOMB_POWER_SMALL = b.comment("Explosion power of a small bomb.").defineInRange("smallPower", 1.5, 0.0, 30.0);
        BOMB_POWER = b.comment("Explosion power of a normal bomb.").defineInRange("power", 3.0, 0.0, 30.0);
        BOMB_POWER_BIG = b.comment("Explosion power of a big bomb.").defineInRange("bigPower", 6.0, 0.0, 30.0);
        BOMB_BREAK_BLOCKS = b.comment("Whether bomb explosions destroy blocks.").define("breakBlocks", true);
        b.pop();
        SPEC = b.build();
    }

    private AerovoxConfig() {}
}
