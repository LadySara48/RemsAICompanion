package org.hearlov.remscompanion;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.hearlov.remscompanion.companion.*;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.io.IOException;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class CompanionVoices {
    static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rems_ai_companion/voice.json");
    static VoiceSettings settings;
    static boolean loadFailed;
    private static final ArrayDeque<String> QUEUE = new ArrayDeque<>();
    private static SimpleSoundInstance playing;
    private static String playingEvent;
    private static long nextPlayback;
    private static final long START = System.nanoTime();
    static final CompanionBehaviour BEHAVIOUR = new CompanionBehaviour(
            () -> (System.nanoTime() - START) / 1_000_000, () -> ThreadLocalRandom.current().nextDouble(), CompanionVoices::enqueue);
    static {
        try { settings = VoiceSettings.load(PATH); }
        catch (IOException ex) { settings = new VoiceSettings(); loadFailed = true; }
    }

    /*
     * Rem'in bir sesini sıraya ekler; ekran kapalıysa, ses kısıksa
     * ya da sırada zaten 3 ses varsa eklemez.
     */
    private static void enqueue(String event) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.gui.screen() instanceof CompanionScreen) || !settings.enabled || settings.volume == 0 || QUEUE.size() >= 3) return;
        QUEUE.addLast(event);
    }

    /*
     * Yazı yazılınca uyku sesini keser ve davranışa haber verir.
     */
    static void input(boolean inserted) {
        if ("sleep".equals(playingEvent)) stop(Minecraft.getInstance());
        BEHAVIOUR.input(inserted);
    }

    /*
     * Her tick'te davranışı günceller, sıradaki sesi uygun zamanda çalar.
     */
    static void tick(Minecraft client) {
        if (client.gui.screen() instanceof VoiceScreen) return;
        boolean visible = client.gui.screen() instanceof CompanionScreen;
        BEHAVIOUR.tick(ChatSession.selected != null && ChatSession.selected.busy, visible);
        if (!visible || !settings.enabled || settings.volume == 0) { stop(client); return; }
        if (playing != null && client.getSoundManager().isActive(playing)) return;
        if (QUEUE.isEmpty() || System.nanoTime() < nextPlayback) return;
        play(client, QUEUE.removeFirst(), settings);
    }

    /*
     * Olayın sesini seçilen dil ve ses seviyesiyle çalar, öncekini durdurur.
     */
    static void play(Minecraft client, String event, VoiceSettings config) {
        if (!config.enabled || config.volume == 0) return;
        if (playing != null) client.getSoundManager().stop(playing);
        String suffix = event.equals("hehe") || event.equals("sleep") ? "" : "_" + config.suffix(client.options.languageCode);
        SoundEvent sound = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(RemsAICompanionClient.MOD_ID, "voice." + event + suffix));
        playing = SimpleSoundInstance.forUI(sound, 1.0F, config.volume / 100.0F);
        playingEvent = event;
        client.getSoundManager().play(playing);
        nextPlayback = System.nanoTime() + 1_000_000_000L;
    }

    /*
     * Sırayı boşaltır ve çalan sesi durdurur.
     */
    static void stop(Minecraft client) {
        QUEUE.clear();
        if (playing != null) { client.getSoundManager().stop(playing); playing = null; }
        playingEvent = null;
    }
}
