package org.hearlov.remscompanion;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
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
     * açılışta sohbetleri yüklüyorum, oyun kapanırken shutdown bağlı.
     * R tuşunu kaydediyorum; her tick'te oturum ve sesler dönüyor,
     * dünyadaysan ve açık ekran yoksa R ana ekranı açıyor
     */
    public void onInitializeClient() {
        ChatSession.load();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ChatSession.shutdown());
        /*PRE219*/var category = @KEY_CATEGORY@;/*ENDPRE219*/
        /*POST219*/var category = KeyMapping.Category.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));/*ENDPOST219*/
        var key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.rems_ai_companion.open_menu", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_R, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatSession.tick(); CompanionVoices.tick(client);
            while (key.consumeClick()) if (client.level != null && client.screen == null) client.setScreen(new CompanionScreen());
        });
        LOGGER.info("Rem's AI Companion initialized.");
    }
}
