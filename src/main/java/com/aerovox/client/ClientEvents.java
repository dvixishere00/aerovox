package com.aerovox.client;

import com.aerovox.AerovoxMod;
import com.aerovox.network.ControlInputPayload;
import com.aerovox.vehicle.AerovoxVehicle;
import com.aerovox.vehicle.FlightPhysics;
import com.aerovox.vehicle.WindSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = AerovoxMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static int lastSentMask = 0;

    private ClientEvents() {}

    /** Sends the pilot's keys to the server when they change. Keys are bound to actions on the server. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int mask = 0;
        if (mc.player.getVehicle() instanceof AerovoxVehicle && mc.screen == null) {
            Options o = mc.options;
            if (isDown(o.keyUp)) mask |= 1 << 0;
            if (isDown(o.keyDown)) mask |= 1 << 1;
            if (isDown(o.keyLeft)) mask |= 1 << 2;
            if (isDown(o.keyRight)) mask |= 1 << 3;
            if (isDown(o.keyJump)) mask |= 1 << 4;
            if (isDown(o.keySprint)) mask |= 1 << 5;
        }
        if (mask != lastSentMask) {
            PacketDistributor.sendToServer(new ControlInputPayload(mask));
            lastSentMask = mask;
        }
    }

    /**
     * While piloting, the character does not move: the movement keys drive the aircraft instead.
     * Keys are still read above, so the controls keep working.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof AerovoxVehicle)) return;
        Input input = event.getInput();
        input.forwardImpulse = 0.0f;
        input.leftImpulse = 0.0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
    }

    /** F5 cycling is vanilla (first person, chase, front). While piloting, the camera rolls and pitches with the aircraft. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof AerovoxVehicle vehicle)) return;
        double[] att = vehicle.attitude();
        event.setRoll((float) att[2]);
        event.setPitch(event.getPitch() - (float) att[1]);
    }

    /** Cockpit HUD: airspeed, ground speed, altitude, heading, attitude, throttle, wind, and stall warning. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (!(mc.player.getVehicle() instanceof AerovoxVehicle vehicle)) return;

        Vec3 wind = WindSystem.sample(mc.level);
        Vec3 velocity = vehicle.getDeltaMovement();
        double airspeed = FlightPhysics.airspeed(vehicle.getOrientation(), velocity, wind) * 20.0;
        double ground = velocity.length() * 20.0;
        double[] att = vehicle.attitude();
        double heading = ((att[0] % 360.0) + 360.0) % 360.0;
        double windSpeed = wind.length() * 20.0;
        double windFrom = ((Math.toDegrees(Math.atan2(-wind.x, wind.z)) % 360.0) + 360.0) % 360.0;

        List<String> lines = List.of(
                String.format("Airspeed  %.0f b/s", airspeed),
                String.format("Ground    %.0f b/s", ground),
                String.format("Altitude  %.1f", vehicle.getY()),
                String.format("Heading   %03.0f deg", heading),
                String.format("Pitch     %+.0f deg", att[1]),
                String.format("Roll      %+.0f deg", att[2]),
                String.format("Throttle  %.0f%%", vehicle.getThrottle() * 100.0),
                String.format("Wind      %.1f b/s from %03.0f deg", windSpeed, windFrom));

        GuiGraphics g = event.getGuiGraphics();
        int y = 6;
        for (String line : lines) {
            g.drawString(mc.font, line, 6, y, 0xFFFFFF, true);
            y += 10;
        }
        if (vehicle.isStalled()) {
            g.drawString(mc.font, "STALL", 6, y + 4, 0xFF5555, true);
        }
    }

    private static boolean isDown(KeyMapping key) {
        return key.isDown();
    }
}
