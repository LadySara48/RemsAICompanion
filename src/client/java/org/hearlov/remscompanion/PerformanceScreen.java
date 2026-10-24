package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.hearlov.remscompanion.settings.PerformanceSettings;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class PerformanceScreen extends MinimalScreen {
    private final Screen parent;
    private PerformanceSettings.Context chosen = AppState.performance.context;
    private boolean error;
    /*
     * Performans ekranını kurar, geri dönülecek ekranı saklar.
     */
    PerformanceScreen(Screen parent) { super(SettingsScreen.label("ai_performance")); this.parent = parent; }
    /*
     * Bağlam boyutunu değiştiren buton ile kaydet ve vazgeç butonlarını kurar.
     * Kaydedilemezse eski değere geri döner.
     */
    @Override protected void init() {
        int w = Math.min(340, width - 20), x = (width - w) / 2;
        addRenderableWidget(new MinimalButton(x, 55, w, 24,
                net.minecraft.network.chat.Component.literal(chosen.label + " (" + chosen.characters / 1000 + "K characters)"), () -> {
            chosen = PerformanceSettings.Context.values()[(chosen.ordinal() + 1) % PerformanceSettings.Context.values().length]; rebuildWidgets();
        }));
        addRenderableWidget(new MinimalButton(x, height - 54, w, 20, SettingsScreen.label("save"), () -> {
            var old = AppState.performance.context; AppState.performance.context = chosen;
            if (AppState.saveSettings()) onClose(); else { AppState.performance.context = old; error = true; }
        }));
        addRenderableWidget(new MinimalButton(x, height - 26, w, 20, SettingsScreen.label("cancel"), this::onClose));
    }

    /*
     * Başlığı ve açıklama ya da hata yazısını çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int w = Math.min(340, width - 20), x = (width - w) / 2;
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 18, 0xFFF0F3F7);
        int y = 98;
        for (var line : font.split(SettingsScreen.label(error ? "save_failed" : "context_note"), w)) { g.text(font, line, x, y, 0xFFA2ABB5); y += 11; }
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
