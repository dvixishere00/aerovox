package com.aerovox.glue;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Keeps glue membership in sync when glued blocks are removed. */
public final class GlueEvents {
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            GlueData.get(level).forgetBlock(event.getPos());
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            GlueData glue = GlueData.get(level);
            for (BlockPos pos : event.getAffectedBlocks()) {
                glue.forgetBlock(pos);
            }
        }
    }
}
