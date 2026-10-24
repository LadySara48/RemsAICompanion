package org.hearlov.remscompanion.compat;
import java.util.WeakHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class Tip {
    private static final WeakHashMap<AbstractWidget, Component> TIPS = new WeakHashMap<>();
    /*
     * metni olduğu gibi döner, set ile beraber tooltip yerine kullanılıyor
     */
    public static Component create(Component text) { return text; }
    /*
     * widget'ın ipucunu kaydediyorum
     */
    public static void set(AbstractWidget widget, Component text) { TIPS.put(widget, text); }
    /*
     * farenin altındaki widget bu ekrandaysa ipucu kutusunu çiziyorum
     */
    public static void render(GuiGraphicsExtractor g, Font font, int mx, int my, int width, int height) {
        for (var entry : TIPS.entrySet()) {
            var widget = entry.getKey();
            if (!widget.visible || !widget.isMouseOver(mx, my)) continue;
            // Only widgets belonging to the currently displayed screen may show a tooltip.
            if (!net.minecraft.client.Minecraft.getInstance().screen.children().contains(widget)) continue;
            var lines = font.split(entry.getValue(), Math.min(300, width - 20));
            int w = lines.stream().mapToInt(font::width).max().orElse(0);
            int x = Math.max(4, Math.min(mx + 8, width - w - 6)); int y = Math.max(4, Math.min(my + 14, height - lines.size() * 11 - 6));
            g.fill(x - 3, y - 3, x + w + 3, y + lines.size() * 11 + 2, 0xF0202530);
            for (var line : lines) { g.text(font, line, x, y, 0xFFF0F3F7); y += 11; }
            return;
        }
    }
}
