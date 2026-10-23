package com.aerovox.client;

import com.aerovox.vehicle.BombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Draws a falling bomb as a TNT block. */
public class BombRenderer extends EntityRenderer<BombEntity> {

    public BombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BombEntity bomb, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5, 0.0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.TNT.defaultBlockState(), poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(bomb, entityYaw, partialTicks, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BombEntity bomb) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
