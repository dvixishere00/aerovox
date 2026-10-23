package com.aerovox.client;

import com.aerovox.AerovoxMod;
import com.aerovox.glue.GlueSelectionView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws the box of the current superglue selection. */
@EventBusSubscriber(modid = AerovoxMod.MODID, value = Dist.CLIENT)
public final class GlueOutlineRenderer {
    private GlueOutlineRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        BlockPos a = GlueSelectionView.first();
        BlockPos b = GlueSelectionView.second();
        if (a == null || b == null) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxX = Math.max(a.getX(), b.getX()) + 1;
        double maxY = Math.max(a.getY(), b.getY()) + 1;
        double maxZ = Math.max(a.getZ(), b.getZ()) + 1;

        VertexConsumer lines = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, minX, minY, minZ, maxX, maxY, maxZ, 0.3f, 0.8f, 1.0f, 1.0f);
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        pose.popPose();
    }
}
