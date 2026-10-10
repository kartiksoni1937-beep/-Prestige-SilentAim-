package dev.prestige.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local-only rotation preview for testing target-angle calculations.
 * It does not rotate the player/camera or alter attack packets.
 */
public final class PrestigeClient implements ClientModInitializer {
    public static final String MOD_ID = "prestige_silentaim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleKey;
    private static boolean previewEnabled;
    private static int lastPreviewTick = -1;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prestige_silentaim.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.prestige_silentaim"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(PrestigeClient::onClientTick);
        LOGGER.info("Prestige rotation preview initialized. Press G to toggle.");
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            previewEnabled = !previewEnabled;
            lastPreviewTick = -1;
            if (client.player != null) {
                client.player.sendMessage(Text.literal(
                        "Rotation preview " + (previewEnabled ? "enabled" : "disabled")
                ), true);
            }
            LOGGER.info("Rotation preview {}", previewEnabled ? "enabled" : "disabled");
        }

        if (!previewEnabled || client.player == null || client.world == null
                || client.currentScreen != null || !client.options.attackKey.isPressed()) {
            return;
        }
        if (lastPreviewTick == client.player.age) {
            return;
        }

        LivingEntity target = findTarget(client);
        if (target == null) {
            client.player.sendMessage(Text.literal("Rotation preview: no visible target in range"), true);
            lastPreviewTick = client.player.age;
            return;
        }

        Vec3d delta = target.getEyePos().subtract(client.player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
        float targetPitch = (float) -(Math.atan2(delta.y, horizontal) * 180.0 / Math.PI);
        float yawDelta = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        float pitchDelta = targetPitch - client.player.getPitch();
        double distance = client.player.distanceTo(target);

        client.player.sendMessage(Text.literal(String.format(
                java.util.Locale.ROOT,
                "ROTATION PREVIEW | %s | %.1fm | target yaw %.1f pitch %.1f | delta %.1f / %.1f",
                target.getName().getString(), distance, targetYaw, targetPitch, yawDelta, pitchDelta
        )), true);

        LOGGER.info("Rotation preview target={} distance={} cameraYaw={} cameraPitch={} targetYaw={} targetPitch={} yawDelta={} pitchDelta={}",
                target.getName().getString(), distance, client.player.getYaw(), client.player.getPitch(),
                targetYaw, targetPitch, yawDelta, pitchDelta);
        lastPreviewTick = client.player.age;
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
