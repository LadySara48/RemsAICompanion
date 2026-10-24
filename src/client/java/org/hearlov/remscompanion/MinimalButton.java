package org.hearlov.remscompanion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class MinimalButton extends Button {
    boolean selected;
    /*
     * Tıklanınca verilen işi çalıştıran sade bir buton oluşturur.
     */
    MinimalButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label, b -> action.run(), DEFAULT_NARRATION);
    }

    /*
     * Butonu sade stilde çizer, sığmayan yazıyı üç noktayla kısaltır.
     */
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float dt) {
        boolean highlight = active && isHoveredOrFocused();
        if (highlight || selected) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), selected ? 0x208FCFE8 : 0x16FFFFFF);
        if (selected) g.fill(getX(), getY() + 4, getX() + 2, getY() + getHeight() - 4, 0xFFADD8EA);
        if (isFocused() && active) g.fill(getX() + 3, getY() + getHeight() - 1, getX() + getWidth() - 3, getY() + getHeight(), 0xFFADD8EA);
        var font = Minecraft.getInstance().font;
        String full = getMessage().getString();
        int available = Math.max(0, getWidth() - 8);
        String text = font.width(full) <= available ? full
                : font.plainSubstrByWidth(full, Math.max(0, available - font.width("…"))) + "…";
        g.text(font, text, getX() + (getWidth() - font.width(text)) / 2, getY() + (getHeight() - 8) / 2,
                selected || highlight ? 0xFFF2F7FA : active ? 0xFFD4D9DF : 0xFF737B85, false);
    }
}
