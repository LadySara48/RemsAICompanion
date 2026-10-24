package org.hearlov.remscompanion;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.hearlov.remscompanion.prompt.*;
import java.io.IOException;
import java.nio.file.Path;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class Prompts {
    static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rems_ai_companion/prompts.json");
    static PromptStore store;
    static boolean loadFailed;
    static {
        try { store = PromptStore.load(PATH); }
        catch (IOException ex) { store = new PromptStore(); loadFailed = true; }
    }

    /*
     * Boyut, koordinat, gizlenmiş sunucu, sürüm ve loader bilgisinden oyun bağlamını oluşturur.
     */
    static PromptComposer.GameContext game(Minecraft client) {
        var player = client.player; var server = client.getCurrentServer();
        return new PromptComposer.GameContext(client.level == null ? "Unavailable" : client.level.dimension().identifier().toString(),
                player == null ? 0 : player.getBlockX(), player == null ? 0 : player.getBlockY(), player == null ? 0 : player.getBlockZ(),
                PromptComposer.serverLabel(server == null ? null : server.ip, client.getSingleplayerServer() != null),
                SharedConstants.getCurrentVersion().name(), "Fabric");
    }

    /*
     * Seçili prompt, değişkenler, oyuncu adı ve oyun diliyle son sistem promptunu oluşturur.
     */
    static String compose(PromptStore settings, Minecraft client) {
        String player = client.player == null ? "Player" : client.player.getName().getString();
        return PromptComposer.compose(settings.template(), settings.variables(), player, client.options.languageCode, game(client));
    }
}
