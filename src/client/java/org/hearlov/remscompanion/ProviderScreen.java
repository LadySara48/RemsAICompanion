package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.api.AiProfile;
import java.util.function.Consumer;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class ProviderScreen extends MinimalScreen {
    private final Screen parent;
    private final Consumer<AiProfile.Provider> selected;
    /*
     * Sağlayıcı seçme ekranını kurar, seçimi geri bildirecek fonksiyonu saklar.
     */
    ProviderScreen(Screen parent, Consumer<AiProfile.Provider> selected) {
        super(Component.translatable("screen.rems_ai_companion.provider_title")); this.parent = parent; this.selected = selected;
    }

    /*
     * Her sağlayıcı için iki sütunlu butonlar dizer, tıklanınca seçip kapanır.
     */
    @Override protected void init() {
        int w = Math.min(400, width - 20), x = (width - w) / 2;
        AiProfile.Provider[] options = {AiProfile.Provider.GEMINI, AiProfile.Provider.DEEPSEEK, AiProfile.Provider.GROK,
                AiProfile.Provider.COMPATIBLE, AiProfile.Provider.KIMI, AiProfile.Provider.OPENROUTER, AiProfile.Provider.VERCEL};
        for (int i = 0; i < options.length; i++) {
            var provider = options[i];
            var label = Component.literal(provider == AiProfile.Provider.COMPATIBLE ? "ChatGPT / Custom" : provider.label);
            var button = new MinimalButton(x + (i % 2) * (w / 2 + 2), 42 + (i / 2) * 28, w / 2 - 2, 22, label, () -> {
                selected.accept(provider); onClose();
            });
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(provider.baseUrl)));
            addRenderableWidget(button);
        }
        addRenderableWidget(new MinimalButton(x, height - 26, w, 20,
                Component.translatable("screen.rems_ai_companion.back"), this::onClose));
    }

    /*
     * Başlığı ve web arama notunu çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int w = Math.min(400, width - 20), x = (width - w) / 2;
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 16, 0xFFF0F3F7);
        int y = 162;
        for (var line : font.split(Component.translatable("screen.rems_ai_companion.gateway_search_note"), w)) {
            if (y >= height - 32) break; g.text(font, line, x, y, 0xFFA2ABB5); y += 10;
        }
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
