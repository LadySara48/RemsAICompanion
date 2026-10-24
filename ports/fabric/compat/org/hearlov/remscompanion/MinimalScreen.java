package org.hearlov.remscompanion;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.compat.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
abstract class MinimalScreen extends Screen {
    /*
     * başlığı direkt üst sınıfa veriyorum
     */
    protected MinimalScreen(Component title) { super(title); }
    /*
     * bu ekranlar açıkken oyun durmasın
     */
    public boolean isPauseScreen() { return false; }
    /*
     * eski sürümde arka plan karartması olmasın diye boş bıraktım
     */
    /*OLD*/@Override public void renderBackground(com.mojang.blaze3d.vertex.PoseStack g) {}/*ENDOLD*/
    /*MODERN*/
    /*
     * 1.20 için aynısı, karartma yok
     */
    /*PRE21*/@Override public void renderBackground(net.minecraft.client.gui.GuiGraphics g) {}/*ENDPRE21*/
    /*
     * 1.21 ve sonrası, sadece imza değişti yine boş
     */
    /*POST21*/@Override public void renderBackground(net.minecraft.client.gui.GuiGraphics g, int x, int y, float dt) {}/*ENDPOST21*/
    /*ENDMODERN*/
    /*
     * widget'ları silip init'i baştan çalıştırıyor, ekranı yeniden kurmak için
     */
    protected void rebuildWidgets() { clearWidgets(); init(); }
    /*
     * açılışta verilen widget'a odak veriyorum,
     * eski sürümde odağı kutunun kendisine de elle söylemek gerekiyor
     */
    public void setInitialFocus(net.minecraft.client.gui.components.events.GuiEventListener widget) {
        setFocused(widget);
        /*OLD*/if (widget instanceof MultiLineEditBox box) box.setFocused(true); else if (widget instanceof EditBox box) box.setFocus(true);/*ENDOLD*/
        /*MODERN*/widget.setFocused(true);/*ENDMODERN*/
    }
    /*OLD*/
    /*
     * eski sürümde odak değişince önceki kutu kendini kapatmıyor,
     * o yüzden eskisini kapatıp yenisini elle açıyorum
     */
    @Override public void setFocused(net.minecraft.client.gui.components.events.GuiEventListener widget) {
        var previous = getFocused();
        if (previous instanceof MultiLineEditBox box && previous != widget) box.setFocused(false);
        if (previous instanceof EditBox box && previous != widget) box.setFocus(false);
        super.setFocused(widget);
        if (widget instanceof MultiLineEditBox box) box.setFocused(true);
        if (widget instanceof EditBox box) box.setFocus(true);
    }
    /*ENDOLD*/
    /*
     * minecraft'ın render çağrısını bizim extractRenderState'e yönlendiriyorum
     */
    public void render(@NATIVE_GRAPHICS@ nativeGraphics, int x, int y, float dt) {
        extractRenderState(new GuiGraphicsExtractor(nativeGraphics), x, y, dt);
    }

    /*
     * önce ekranın kendisini çiziyorum, tooltip'ler en üste geliyor
     */
    public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float dt) {
        super.render(graphics.nativeGraphics, x, y, dt);
        Tip.render(graphics, font, x, y, width, height);
    }

    /*
     * bizim KeyEvent'i sürümün beklediği keyPressed'e çeviriyorum
     */
    public boolean keyPressed(KeyEvent event) {
        /*LEGACY_INPUT*/return super.keyPressed(event.key(), event.scanCode(), event.modifiers());/*ENDLEGACY_INPUT*/
        /*NEW_INPUT*/return super.keyPressed(new net.minecraft.client.input.KeyEvent(event.key(), event.scanCode(), event.modifiers()));/*ENDNEW_INPUT*/
    }

    /*
     * tıklamayı sürüme uygun mouseClicked'e aktarıyorum
     */
    public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        /*LEGACY_INPUT*/return super.mouseClicked(event.x(), event.y(), event.button());/*ENDLEGACY_INPUT*/
        /*NEW_INPUT*/return super.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(event.x(), event.y(), new net.minecraft.client.input.MouseButtonInfo(event.button(), 0)), twice);/*ENDNEW_INPUT*/
    }

    /*
     * fare bırakmayı sürüme göre iletiyorum
     */
    public boolean mouseReleased(MouseButtonEvent event) {
        /*LEGACY_INPUT*/return super.mouseReleased(event.x(), event.y(), event.button());/*ENDLEGACY_INPUT*/
        /*NEW_INPUT*/return super.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(event.x(), event.y(), new net.minecraft.client.input.MouseButtonInfo(event.button(), 0)));/*ENDNEW_INPUT*/
    }

    /*
     * sürüklemeyi de aynı şekilde sürüme göre iletiyorum
     */
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        /*LEGACY_INPUT*/return super.mouseDragged(event.x(), event.y(), event.button(), dx, dy);/*ENDLEGACY_INPUT*/
        /*NEW_INPUT*/return super.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(event.x(), event.y(), new net.minecraft.client.input.MouseButtonInfo(event.button(), 0)), dx, dy);/*ENDNEW_INPUT*/
    }
    /*LEGACY_INPUT*/
    /*
     * eski tuş imzasını KeyEvent'e sarıp ortak metoda yolluyorum
     */
    @Override public boolean keyPressed(int key, int scan, int mods) { return keyPressed(new KeyEvent(key, scan, mods)); }
    /*
     * eski tıklama imzası, ortak mouseClicked'e gidiyor
     */
    @Override public boolean mouseClicked(double x, double y, int button) { return mouseClicked(new MouseButtonEvent(x, y, button), false); }
    /*
     * eski bırakma imzası, ortak metoda bağlı
     */
    @Override public boolean mouseReleased(double x, double y, int button) { return mouseReleased(new MouseButtonEvent(x, y, button)); }
    /*
     * eski sürükleme imzası, ortak metoda bağlı
     */
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return mouseDragged(new MouseButtonEvent(x, y, button), dx, dy); }
    /*ENDLEGACY_INPUT*/
    /*NEW_INPUT*/
    /*
     * yeni KeyEvent'i bizim record'a çevirip ortak metoda yolluyorum
     */
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e) { return keyPressed(new KeyEvent(e.key(), e.scancode(), e.modifiers())); }
    /*
     * yeni tıklama olayı, bizim MouseButtonEvent'e çeviriyorum
     */
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean twice) { return mouseClicked(new MouseButtonEvent(e.x(), e.y(), e.button()), twice); }
    /*
     * yeni bırakma olayı için aynısı
     */
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e) { return mouseReleased(new MouseButtonEvent(e.x(), e.y(), e.button())); }
    /*
     * yeni sürükleme olayı için aynısı
     */
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e, double dx, double dy) { return mouseDragged(new MouseButtonEvent(e.x(), e.y(), e.button()), dx, dy); }
    /*ENDNEW_INPUT*/
    /*PRE21*/
    /*
     * 1.21 öncesinde yatay kaydırma yok, sadece dikeyi üst sınıfa veriyorum
     */
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) { return super.mouseScrolled(x, y, vertical); }
    /*
     * eski tek değerli kaydırmayı dört parametreli olana bağlıyorum
     */
    @Override public boolean mouseScrolled(double x, double y, double amount) { return mouseScrolled(x, y, 0, amount); }
    /*ENDPRE21*/
}
