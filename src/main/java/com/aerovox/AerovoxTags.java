package com.aerovox;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class AerovoxTags {
    public static final TagKey<Block> ENGINES = block("engines");
    public static final TagKey<Block> CONTROL_SURFACES = block("control_surfaces");
    public static final TagKey<Block> LIFTING_SURFACES = block("lifting_surfaces");
    public static final TagKey<Block> LANDING_GEAR = block("landing_gear");

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AerovoxMod.MODID, name));
    }

    private AerovoxTags() {}
}
