package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class SettingsScreen extends MinimalScreen {
    private final Screen parent;
    /*
     * Genel ayarlar ekranını kurar, geri dönülecek ekranı saklar.
     */
    SettingsScreen(Screen parent) { super(label("general_settings")); this.parent = parent; }
    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    static Component label(String key) { return Component.translatable("screen.rems_ai_companion." + key); }
    /*
     * Ses, yapay zeka performansı ve istatistik ekranlarına giden butonları dizer.
     */
    @Override protected void init() {
        int w = Math.min(320, width - 20), x = (width - w) / 2;
        addRenderableWidget(new MinimalButton(x, 48, w, 22, label("voice_settings"), () -> minecraft.gui.setScreen(new VoiceScreen(this))));
        addRenderableWidget(new MinimalButton(x, 80, w, 22, label("ai_performance"), () -> minecraft.gui.setScreen(new PerformanceScreen(this))));
        addRenderableWidget(new MinimalButton(x, 112, w, 22, label("statistics"), () -> minecraft.gui.setScreen(new StatisticsScreen(this))));
        addRenderableWidget(new MinimalButton(x, height - 26, w, 20, label("back"), this::onClose));
    }

    /*
     * Başlığı ortalayıp çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, (width - font.width(title)) / 2, 18, 0xFFF0F3F7);
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
