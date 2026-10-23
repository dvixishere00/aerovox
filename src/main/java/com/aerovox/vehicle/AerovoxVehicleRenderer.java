package com.aerovox.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Draws the structure rotated by the aircraft's quaternion, centered on the center of mass.
 * Rotor and worker blades are drawn turning around their head's axis.
 */
public class AerovoxVehicleRenderer extends EntityRenderer<AerovoxVehicle> {

    public AerovoxVehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AerovoxVehicle vehicle, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        List<AerovoxVehicle.VehicleBlock> blocks = vehicle.getBlocks();
        List<BladeLayout.Blade> layout = vehicle.getBladeLayout();

        boolean[] moving = new boolean[blocks.size()];
        for (BladeLayout.Blade blade : layout) {
            moving[blade.block()] = true;
        }

        poseStack.pushPose();
        poseStack.mulPose(vehicle.getOrientation());

        for (int i = 0; i < blocks.size(); i++) {
            if (!moving[i]) drawBlock(dispatcher, blocks.get(i), poseStack, buffers, packedLight);
        }

        for (BladeLayout.Blade blade : layout) {
            poseStack.pushPose();
            poseStack.translate(blade.px(), blade.py(), blade.pz());
            poseStack.mulPose(Axis.ZP.rotation(angleFor(blade.kind(), vehicle)));
            poseStack.translate(-blade.px(), -blade.py(), -blade.pz());
            drawBlock(dispatcher, blocks.get(blade.block()), poseStack, buffers, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(vehicle, entityYaw, partialTicks, poseStack, buffers, packedLight);
    }

    private static float angleFor(BladeLayout.Kind kind, AerovoxVehicle vehicle) {
        return switch (kind) {
            case ROTOR -> vehicle.getRotorAngle();
            case REVERSED_ROTOR -> -vehicle.getRotorAngle();
            case WORKER -> (float) Math.toRadians(vehicle.getWorkerAngle());
            case INVERTED_WORKER -> (float) -Math.toRadians(vehicle.getWorkerAngle());
        };
    }

    private static void drawBlock(BlockRenderDispatcher dispatcher, AerovoxVehicle.VehicleBlock b,
                                  PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        // Blocks are unit cubes at their corner, so shift by half a block to center them.
        poseStack.translate(b.cx() - 0.5, b.cy() - 0.5, b.cz() - 0.5);
        dispatcher.renderSingleBlock(b.state(), poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AerovoxVehicle vehicle) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
