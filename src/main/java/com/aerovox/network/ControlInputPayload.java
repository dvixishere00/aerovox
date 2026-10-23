package com.aerovox.network;

import com.aerovox.AerovoxMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the pilot's current control keys as a bit mask (see AerovoxVehicle.INPUT_*). */
public record ControlInputPayload(int mask) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ControlInputPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AerovoxMod.MODID, "control_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ControlInputPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ControlInputPayload::mask, ControlInputPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
