package com.aerovox.network;

import com.aerovox.AerovoxMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: new key bindings for an aircraft, packed as in ControlAction.pack. */
public record BindingPayload(int entityId, int bindings) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BindingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AerovoxMod.MODID, "bindings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BindingPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BindingPayload::entityId,
                    ByteBufCodecs.VAR_INT, BindingPayload::bindings, BindingPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
