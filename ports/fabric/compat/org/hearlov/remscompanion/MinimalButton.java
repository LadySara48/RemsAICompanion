package org.hearlov.remscompanion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.compat.GuiGraphicsExtractor;

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
     * butonu kuruyorum, super çağrısını prepare.py sürüme göre dolduruyor
     */
    MinimalButton(int x, int y, int width, int height, Component label, Runnable action) { @BUTTON_SUPER@ }
    /*
     * butonu sade çiziyorum: seçiliyse/üstündeysen hafif zemin, odakta alt çizgi,
     * sığmayan yazı da üç noktayla kısalıyor. metot adı sürüme göre değişiyor
     */
    @Override public void @BUTTON_RENDER@(@NATIVE_GRAPHICS@ nativeGraphics, int mx, int my, float dt) {
        var g = new GuiGraphicsExtractor(nativeGraphics); int bx = @X@, by = @Y@;
        boolean highlight = active && isHoveredOrFocused();
        if (highlight || selected) g.fill(bx, by, bx + getWidth(), by + getHeight(), selected ? 0x208FCFE8 : 0x16FFFFFF);
        if (selected) g.fill(bx, by + 4, bx + 2, by + getHeight() - 4, 0xFFADD8EA);
        if (isFocused() && active) g.fill(bx + 3, by + getHeight() - 1, bx + getWidth() - 3, by + getHeight(), 0xFFADD8EA);
        var font = Minecraft.getInstance().font; String full = getMessage().getString(); int available = Math.max(0, getWidth() - 8);
        String text = font.width(full) <= available ? full : font.plainSubstrByWidth(full, Math.max(0, available - font.width("…"))) + "…";
        g.text(font, text, bx + (getWidth() - font.width(text)) / 2, by + (getHeight() - 8) / 2, selected || highlight ? 0xFFF2F7FA : active ? 0xFFD4D9DF : 0xFF737B85);
    }
}
