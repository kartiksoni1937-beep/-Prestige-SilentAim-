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

public final class PrestigeClient implements ClientModInitializer {
    public static final String MOD_ID = "prestige_silentaim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleKey;
    private static boolean enabled;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prestige_silentaim.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.prestige_silentaim"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(PrestigeClient::onClientTick);
        LOGGER.info("Prestige client initialized. Press G to toggle aim assist.");
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            LOGGER.info("Aim assist {}", enabled ? "enabled" : "disabled");
        }

        if (!enabled || client.player == null || client.world == null
                || client.currentScreen != null || !client.options.attackKey.isPressed()) {
            return;
        }

        LivingEntity target = findTarget(client);
        if (target == null) {
            return;
        }

        Vec3d eyes = client.player.getEyePos();
        Vec3d targetEyes = target.getEyePos();
        Vec3d delta = targetEyes.subtract(eyes);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float targetYaw = (float) (Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
        float targetPitch = (float) -(Math.atan2(delta.y, horizontal) * 180.0 / Math.PI);

        // Smooth, visible client rotation. This does not suppress or spoof packets.
        float yawStep = MathHelper.clamp(MathHelper.wrapDegrees(targetYaw - client.player.getYaw()), -12.0F, 12.0F);
        float pitchStep = MathHelper.clamp(targetPitch - client.player.getPitch(), -8.0F, 8.0F);
        client.player.setYaw(client.player.getYaw() + yawStep);
        client.player.setPitch(MathHelper.clamp(client.player.getPitch() + pitchStep, -90.0F, 90.0F));
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
                                && isWithinAimCone(eyes, look, entity)
                )
                .stream()
                .min((a, b) -> Double.compare(
                        client.player.squaredDistanceTo(a),
                        client.player.squaredDistanceTo(b)))
                .orElse(null);
    }

    private static boolean isWithinAimCone(Vec3d eyes, Vec3d look, LivingEntity entity) {
        Vec3d toTarget = entity.getEyePos().subtract(eyes).normalize();
        // Keep selection near the crosshair so it does not snap to targets behind the player.
        return look.dotProduct(toTarget) > 0.75;
    }
}
