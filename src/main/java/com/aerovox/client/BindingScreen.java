package com.aerovox.client;

import com.aerovox.network.BindingPayload;
import com.aerovox.vehicle.AerovoxVehicle;
import com.aerovox.vehicle.ControlAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Key assignment menu: click a key row to cycle its action, then Save. */
public final class BindingScreen extends Screen {
    private final int entityId;
    private final ControlAction[] actions = new ControlAction[AerovoxVehicle.KEY_COUNT];

    public BindingScreen(int entityId, int packedBindings) {
        super(Component.literal("Controller Bindings"));
        this.entityId = entityId;
        for (int i = 0; i < actions.length; i++) {
            actions[i] = ControlAction.of(packedBindings, i);
        }
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        for (int i = 0; i < actions.length; i++) {
            final int key = i;
            addRenderableWidget(Button.builder(keyLabel(key), b -> {
                actions[key] = ControlAction.values()[(actions[key].ordinal() + 1) % ControlAction.values().length];
                b.setMessage(keyLabel(key));
            }).bounds(left, 40 + i * 24, 200, 20).build());
        }
        int bottom = 40 + actions.length * 24 + 12;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            PacketDistributor.sendToServer(new BindingPayload(entityId, ControlAction.pack(actions)));
            onClose();
        }).bounds(left, bottom, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(left + 102, bottom, 98, 20).build());
    }

    private Component keyLabel(int key) {
        return Component.literal(AerovoxVehicle.KEY_NAMES[key] + ": " + actions[key].label());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
