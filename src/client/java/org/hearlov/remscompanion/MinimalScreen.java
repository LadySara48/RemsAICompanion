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
abstract class MinimalScreen extends Screen {
    /*
     * Başlığı üst sınıfa ileten kurucu.
     */
    protected MinimalScreen(Component title) { super(title); }
    /*
     * Vanilla'nın karartma ve bulanıklığını istemediğim için arka planı boş bırakır.
     */
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float dt) {
        // The screen draws one translucent surface; avoid vanilla's extra dim/blur layers.
    }

    /*
     * Bu ekranlar açıkken oyun durmasın diye false döner.
     */
    @Override public boolean isPauseScreen() { return false; }
}
