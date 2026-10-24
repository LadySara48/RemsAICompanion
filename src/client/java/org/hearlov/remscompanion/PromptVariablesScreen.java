package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import org.hearlov.remscompanion.prompt.PromptStore;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class PromptVariablesScreen extends MinimalScreen {
    private final PromptScreen parent;
    private int x, w;
    /*
     * Değişkenler ekranını kurar, prompt ekranını saklar.
     */
    PromptVariablesScreen(PromptScreen parent) { super(PromptScreen.label("variables")); this.parent = parent; }
    /*
     * Bir değişken için yazı kutusu ekler, değişince ayarı günceller.
     */
    private void field(String key, String value, int y, int limit, java.util.function.Consumer<String> setter) {
        EditBox box = new EditBox(font, x, y, w, 18, PromptScreen.label(key));
        box.setMaxLength(limit); box.setValue(value); box.setResponder(setter); addRenderableWidget(box);
    }

    /*
     * İsim, yaş, oyuncu adı, dil ve davranış alanlarını kurar.
     */
    @Override protected void init() {
        x = Math.max(10, (width - 420) / 2); w = Math.min(420, width - 20);
        PromptStore.Variables v = parent.draft.variables();
        field("character_name", v.name, 40, 80, value -> v.name = value);
        field("character_age", v.age, 68, 40, value -> v.age = value);
        field("player_name", v.playerName, 96, 80, value -> v.playerName = value);
        field("prompt_language", v.language, 124, 80, value -> v.language = value);
        MultiLineEditBox behaviour = MultiLineEditBox.builder().setX(x).setY(153)
                .build(font, w, Math.max(30, height - 190), PromptScreen.label("behaviour"));
        behaviour.setCharacterLimit(2000); behaviour.setValue(v.behaviour); behaviour.setValueListener(value -> v.behaviour = value);
        addRenderableWidget(behaviour);
        addRenderableWidget(new MinimalButton(x, height - 26, w, 20, PromptScreen.label("back"), this::onClose));
    }

    /*
     * Başlığı ve alan etiketlerini çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 10, 0xFFF0F3F7);
        String[] keys = {"character_name", "character_age", "player_name", "prompt_language", "behaviour"};
        for (int i = 0; i < keys.length; i++) g.text(font, PromptScreen.label(keys[i]), x, 30 + i * 28, 0xFFE0E9F5);
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    /*
     * Bu ekran açıkken oyun durmasın diye false döner.
     */
    @Override public boolean isPauseScreen() { return false; }
}

