package com.aerovox.network;

import com.aerovox.AerovoxMod;
import com.aerovox.vehicle.AerovoxVehicle;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AerovoxNetwork {
    private AerovoxNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AerovoxMod.MODID);

        registrar.playToServer(ControlInputPayload.TYPE, ControlInputPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.getVehicle() instanceof AerovoxVehicle vehicle) {
                        vehicle.setControlMask(payload.mask());
                    }
                }));

        registrar.playToServer(BindingPayload.TYPE, BindingPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.level().getEntity(payload.entityId()) instanceof AerovoxVehicle vehicle
                            && vehicle.distanceToSqr(player) < 256.0
                            && vehicle.hasController()) {
                        vehicle.setBindings(payload.bindings());
                    }
                }));
    }
}
