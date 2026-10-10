package dev.prestige.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes a separate, hypothetical target rotation for controlled testing.
 * It never changes camera/player angles and never modifies outgoing packets.
 */
public final class PrestigeClient implements ClientModInitializer {
    public static final String MOD_ID = "prestige_silentaim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleKey;
    private static boolean diagnosticsEnabled;
    private static int lastLoggedTick = -1;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prestige_silentaim.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.prestige_silentaim"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(PrestigeClient::onClientTick);
        LOGGER.info("Prestige rotation calculator initialized. Press G to toggle diagnostics.");
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            diagnosticsEnabled = !diagnosticsEnabled;
            lastLoggedTick = -1;
            LOGGER.info("Rotation diagnostics {}", diagnosticsEnabled ? "enabled" : "disabled");
        }

        if (!diagnosticsEnabled || client.player == null || client.world == null
                || client.currentScreen != null || !client.options.attackKey.isPressed()) {
            return;
        }

        LivingEntity target = findTarget(client);
        if (target == null || lastLoggedTick == client.player.age) {
            return;
        }

        // Separate calculated state: these values are for diagnostics only.
        Vec3d delta = target.getEyePos().subtract(client.player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float calculatedYaw = (float) (Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
        float calculatedPitch = (float) -(Math.atan2(delta.y, horizontal) * 180.0 / Math.PI);
        float yawDelta = MathHelper.wrapDegrees(calculatedYaw - client.player.getYaw());
        float pitchDelta = calculatedPitch - client.player.getPitch();

        LOGGER.info("Rotation diagnostic target={} distance={} cameraYaw={} cameraPitch={} calculatedYaw={} calculatedPitch={} yawDelta={} pitchDelta={}",
                target.getName().getString(),
                String.format(java.util.Locale.ROOT, "%.2f", client.player.distanceTo(target)),
                client.player.getYaw(), client.player.getPitch(),
                calculatedYaw, calculatedPitch, yawDelta, pitchDelta);
        lastLoggedTick = client.player.age;
    }

    private static LivingEntity findTarget(MinecraftClient client) {
        Vec3d eyes = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1.0F);
        return client.world.getEntitiesByClass(
                        LivingEntity.class,
                        client.player.getBoundingBox().expand(6.0),
                        entity -> entity != client.player
                                && entity.isAlive()
                                && !entity.isSpectator()
                                && client.player.canSee(entity)
                                && look.dotProduct(entity.getEyePos().subtract(eyes).normalize()) > 0.75
                )
                .stream()
                .min((a, b) -> Double.compare(
                        client.player.squaredDistanceTo(a),
                        client.player.squaredDistanceTo(b)))
                .orElse(null);
    }
}
