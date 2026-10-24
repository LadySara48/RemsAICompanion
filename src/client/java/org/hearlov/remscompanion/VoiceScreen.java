package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.companion.VoiceSettings;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class VoiceScreen extends MinimalScreen {
    private final Screen parent;
    private final VoiceSettings draft = CompanionVoices.settings.copy();
    private String error = "";
    private int x, w;
    /*
     * Ses ayarları ekranını kurar, geri dönülecek ekranı saklar.
     */
    VoiceScreen(Screen parent) { super(label("voice_settings")); this.parent = parent; }
    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    private static Component label(String key) { return Component.translatable("screen.rems_ai_companion." + key); }
    /*
     * Sade bir buton ekler.
     */
    private void button(Component label, int bx, int y, int bw, Runnable action) {
        addRenderableWidget(new MinimalButton(bx, y, bw, 20, label, action));
    }

    /*
     * Ses aç/kapa, dil, ses seviyesi, dene, kaydet ve vazgeç butonlarını kurar.
     */
    @Override protected void init() {
        x = Math.max(10, (width - 360) / 2); w = Math.min(360, width - 20);
        button(label(draft.enabled ? "voice_on" : "voice_off"), x, 48, w, () -> {
            draft.enabled = !draft.enabled; CompanionVoices.stop(minecraft); rebuildWidgets();
        });
        button(label("voice_language_" + draft.language.name().toLowerCase(java.util.Locale.ROOT)), x, 78, w, () -> {
            draft.language = VoiceSettings.Language.values()[(draft.language.ordinal() + 1) % VoiceSettings.Language.values().length];
            CompanionVoices.stop(minecraft); rebuildWidgets();
        });
        button(Component.literal("−"), x, 108, 40, () -> { draft.volume = Math.max(0, draft.volume - 10); rebuildWidgets(); });
        button(Component.literal("+"), x + w - 40, 108, 40, () -> { draft.volume = Math.min(100, draft.volume + 10); rebuildWidgets(); });
        button(label("voice_preview"), x, 142, w, () -> CompanionVoices.play(minecraft, "start_thinking", draft));
        button(label("save"), x, height - 26, w / 2 - 2, () -> {
            if (CompanionVoices.loadFailed) { error = "voice_load_failed"; return; }
            try { draft.save(CompanionVoices.PATH); CompanionVoices.settings = draft; onClose(); }
            catch (java.io.IOException ex) { error = "voice_save_failed"; }
        });
        button(label("cancel"), x + w / 2 + 2, height - 26, w / 2 - 2, this::onClose);
    }

    /*
     * Başlığı, ses seviyesini ve alttaki notu çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 15, 0xFFF0F3F7);
        Component volume = Component.translatable("screen.rems_ai_companion.voice_volume", draft.volume);
        g.text(font, volume, x + (w - font.width(volume)) / 2, 114, 0xFFD4D9DF);
        Component notice = label(CompanionVoices.loadFailed ? "voice_load_failed" : error.isEmpty() ? "voice_auto_note" : error);
        int y = 174;
        for (var line : font.split(notice, w)) { if (y >= height - 35) break; g.text(font, line, x, y, 0xFFA2ABB5); y += 10; }
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Çalan sesi durdurup önceki ekrana döner.
     */
    @Override public void onClose() { CompanionVoices.stop(minecraft); minecraft.gui.setScreen(parent); }
    /*
     * Ekran kapanırken çalan sesi keser.
     */
    @Override public void removed() { CompanionVoices.stop(minecraft); super.removed(); }
}
