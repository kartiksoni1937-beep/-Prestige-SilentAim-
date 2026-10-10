package dev.prestige.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrestigeClient implements ClientModInitializer {
    public static final String MOD_ID = "prestige_silentaim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Prestige SilentAim client initialized.");
        LOGGER.info("Build milestone: Fabric client scaffold; local diagnostics can be added next.");
    }
}
