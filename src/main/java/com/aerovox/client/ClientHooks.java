package com.aerovox.client;

import net.minecraft.client.Minecraft;

/** Entry points for client-only UI, called only from client-side code paths. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openBindings(int entityId, int packedBindings) {
        Minecraft.getInstance().setScreen(new BindingScreen(entityId, packedBindings));
    }
}
