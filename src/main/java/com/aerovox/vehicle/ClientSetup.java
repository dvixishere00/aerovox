package com.aerovox.vehicle;

import com.aerovox.AerovoxMod;
import com.aerovox.AerovoxRegistries;
import com.aerovox.client.BombRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AerovoxMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AerovoxRegistries.VEHICLE.get(), AerovoxVehicleRenderer::new);
        event.registerEntityRenderer(AerovoxRegistries.BOMB.get(), BombRenderer::new);
    }
}
