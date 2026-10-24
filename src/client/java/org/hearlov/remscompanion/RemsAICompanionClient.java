package org.hearlov.remscompanion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class RemsAICompanionClient implements ClientModInitializer {
    public static final String MOD_ID = "rems_ai_companion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /*
     * Mod açılırken sohbetleri yükler, kapanışta kaydı bağlar ve R tuşunu kaydeder.
     * Her tick'te kayıt ve ses işlerini çalıştırır, tuşa basılınca ana ekranı açar.
     */
    @Override
    public void onInitializeClient() {
        ChatSession.load();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ChatSession.shutdown());
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        KeyMapping openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rems_ai_companion.open_menu", InputConstants.KEY_R, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatSession.tick();
            CompanionVoices.tick(client);
            while (openMenu.consumeClick()) {
                if (client.level != null && client.gui.screen() == null) {
                    client.gui.setScreen(new CompanionScreen());
                }
            }
        });
        LOGGER.info("Rem's AI Companion initialized.");
    }
}

