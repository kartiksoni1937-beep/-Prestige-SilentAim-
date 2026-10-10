package dev.prestige.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rotation diagnostics for controlled Fabric/GrimAC compatibility testing.
 * This module deliberately does not alter player rotation or outgoing packets.
 */
public final class PrestigeClient implements ClientModInitializer {
    public static final String MOD_ID = "prestige_silentaim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleKey;
    private static boolean diagnosticsEnabled;
    private static float previousYaw;
    private static float previousPitch;
    private static boolean havePreviousRotation;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prestige_silentaim.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.prestige_silentaim"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(PrestigeClient::onClientTick);
        LOGGER.info("Prestige rotation diagnostics initialized. Press G to toggle diagnostics.");
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            diagnosticsEnabled = !diagnosticsEnabled;
            havePreviousRotation = false;
            LOGGER.info("Rotation diagnostics {}", diagnosticsEnabled ? "enabled" : "disabled");
        }

        if (!diagnosticsEnabled || client.player == null || client.world == null
                || client.currentScreen != null) {
            havePreviousRotation = false;
            return;
        }

        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();
        boolean rotationChanged = !havePreviousRotation
                || Math.abs(yaw - previousYaw) > 0.001F
                || Math.abs(pitch - previousPitch) > 0.001F;

        if (rotationChanged && client.options.attackKey.isPressed()) {
            LOGGER.info("Attack held with player rotation: yaw={}, pitch={}, tick={}",
                    yaw, pitch, client.player.age);
        }

        previousYaw = yaw;
        previousPitch = pitch;
        havePreviousRotation = true;
    }
}
