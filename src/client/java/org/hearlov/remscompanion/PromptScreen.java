package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.prompt.*;
import java.util.ArrayList;
import java.util.List;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class PromptScreen extends MinimalScreen {
    private final Screen parent;
    final PromptStore draft = Prompts.store.copy();
    private MultiLineEditBox editor;
    private int x, w;
    private String error = "";
    /*
     * Prompt ekranını kurar, geri dönülecek ekranı saklar.
     */
    PromptScreen(Screen parent) { super(label("prompts")); this.parent = parent; }
    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    static Component label(String key) { return Component.translatable("screen.rems_ai_companion." + key); }
    /*
     * Buton ekleyip geri döner, sonradan açıp kapatabilmek için.
     */
    private Button button(Component label, int bx, int y, int width, Runnable action) {
        return addRenderableWidget(new MinimalButton(bx, y, width, 20, label, action));
    }

    /*
     * Default ve özel promptlar arasında ileri ya da geri geçer.
     */
    private void cycle(int direction) {
        List<String> ids = new ArrayList<>(); ids.add("default");
        for (PromptStore.Entry p : draft.prompts) ids.add(p.id);
        draft.selected = ids.get(Math.floorMod(ids.indexOf(draft.selected) + direction, ids.size()));
        error = ""; rebuildWidgets();
    }

    /*
     * Prompt değiştirme, kopyalama, silme, değişkenler, isim ve metin kutusunu kurar.
     * Default prompt düzenlenemez, kaydederken boyut kontrol edilir.
     */
    @Override protected void init() {
        error = "";
        x = Math.max(10, (width - 620) / 2); w = Math.min(620, width - 20);
        button(Component.literal("<"), x, 26, 22, () -> cycle(-1));
        button(Component.literal(">"), x + 24, 26, 22, () -> cycle(1));
        button(label("copy_prompt"), x + 50, 26, 70, () -> {
            String template = draft.template();
            PromptStore.Variables variables = draft.copy().variables();
            PromptStore.Entry p = new PromptStore.Entry(); p.name = "Custom " + (draft.prompts.size() + 1);
            p.template = template; p.variables = variables; draft.prompts.add(p); draft.selected = p.id;
            error = ""; rebuildWidgets();
        });
        Button delete = button(label("delete_profile"), x + 124, 26, 60, () -> {
            draft.prompts.remove(draft.activeCustom()); draft.selected = "default"; rebuildWidgets();
        });
        delete.active = draft.activeCustom() != null;
        button(label("variables"), x + w - 96, 26, 96, () -> minecraft.gui.setScreen(new PromptVariablesScreen(this)));
        EditBox name = new EditBox(font, x, 52, w, 18, label("prompt_name"));
        name.setMaxLength(80); name.setValue(draft.name()); name.setEditable(draft.activeCustom() != null);
        name.setResponder(value -> { if (draft.activeCustom() != null) draft.activeCustom().name = value; });
        addRenderableWidget(name);
        editor = MultiLineEditBox.builder().setX(x).setY(75).build(font, w, Math.max(30, height - 205), label("prompt_body"));
        editor.setCharacterLimit(PromptComposer.MAX_BODY); editor.setValue(draft.template());
        editor.setValueListener(value -> {
            error = "";
            if (draft.activeCustom() != null) draft.activeCustom().template = value;
            else if (!value.equals(DefaultPrompt.TEXT)) editor.setValue(DefaultPrompt.TEXT);
        });
        addRenderableWidget(editor);
        button(label("save"), x, height - 26, w / 2 - 2, () -> {
            if (Prompts.loadFailed) { error = "prompt_load_failed"; return; }
            try { Prompts.compose(draft, minecraft); }
            catch (IllegalArgumentException ex) { error = "prompt_too_large"; return; }
            try { draft.save(Prompts.PATH); Prompts.store = draft; onClose(); }
            catch (java.io.IOException ex) { error = "prompt_save_failed"; }
        });
        button(label("cancel"), x + w / 2 + 2, height - 26, w / 2 - 2, this::onClose);
    }

    /*
     * Oyun bilgisi bloğunu, karakter sayacını ya da hata yazısını çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 10, 0xFFF0F3F7);
        int y = height - 125;
        g.text(font, label("game_context_required"), x, y, 0xFFF0F3F7); y += 11;
        for (String line : Prompts.game(minecraft).block().split("\n")) {
            g.text(font, font.plainSubstrByWidth(line, w), x, y, 0xFF9AAEC7); y += 9;
        }
        String problem = error; Component count = Component.empty();
        try {
            int total = Prompts.compose(draft, minecraft).length();
            int body = total - Prompts.game(minecraft).block().length() - 2;
            count = Component.translatable("screen.rems_ai_companion.prompt_count", body, total);
        }
        catch (IllegalArgumentException ex) { problem = "prompt_too_large"; }
        if (!problem.isEmpty() || Prompts.loadFailed) g.text(font, label(Prompts.loadFailed ? "prompt_load_failed" : problem), x, height - 39, 0xFFFF9090);
        else g.text(font, count, x, height - 39, 0xFFE0E9F5);
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

