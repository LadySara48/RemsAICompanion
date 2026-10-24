package org.hearlov.remscompanion.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class GuiGraphicsExtractor {
    public final @NATIVE_GRAPHICS@ nativeGraphics;
    /*
     * sürümün kendi grafik nesnesini sarıyorum
     */
    public GuiGraphicsExtractor(@NATIVE_GRAPHICS@ graphics) { nativeGraphics = graphics; }
    /*
     * dikdörtgen dolduruyor, eski ve yeni api'ye göre
     */
    public void fill(int x1, int y1, int x2, int y2, int color) {
        /*OLD*/net.minecraft.client.gui.GuiComponent.fill(nativeGraphics, x1, y1, x2, y2, color);/*ENDOLD*/
        /*MODERN*/nativeGraphics.fill(x1, y1, x2, y2, color);/*ENDMODERN*/
    }

    /*
     * component'i sıralı metne çevirip çiziyorum
     */
    public void text(Font font, Component text, int x, int y, int color) { text(font, text.getVisualOrderText(), x, y, color); }
    /*
     * gölge parametresi var ama burada yok sayıyorum
     */
    public void text(Font font, Component text, int x, int y, int color, boolean shadow) { text(font, text, x, y, color); }
    /*
     * düz yazı, gölgesiz
     */
    public void text(Font font, String text, int x, int y, int color) { text(font, text, x, y, color, false); }
    /*
     * düz yazı, istenirse gölgeli
     */
    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        /*OLD*/if (shadow) font.drawShadow(nativeGraphics, text, x, y, color); else font.draw(nativeGraphics, text, x, y, color);/*ENDOLD*/
        /*MODERN*/nativeGraphics.drawString(font, text, x, y, color, shadow);/*ENDMODERN*/
    }

    /*
     * hazır satırı gölgesiz çiziyorum
     */
    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        /*OLD*/font.draw(nativeGraphics, text, x, y, color);/*ENDOLD*/
        /*MODERN*/nativeGraphics.drawString(font, text, x, y, color, false);/*ENDMODERN*/
    }

    /*
     * çizimi verilen alana kırpıyorum,
     * eski sürümde ekran ölçeğini elle hesaplamak gerekiyor
     */
    public void enableScissor(int left, int top, int right, int bottom) {
        /*OLD*/var window = Minecraft.getInstance().getWindow(); double scale = window.getGuiScale();
        com.mojang.blaze3d.systems.RenderSystem.enableScissor((int)(left * scale), (int)(window.getHeight() - bottom * scale), (int)((right - left) * scale), (int)((bottom - top) * scale));/*ENDOLD*/
        /*MODERN*/nativeGraphics.enableScissor(left, top, right, bottom);/*ENDMODERN*/
    }

    /*
     * kırpmayı kapatıyor
     */
    public void disableScissor() {
        /*OLD*/com.mojang.blaze3d.systems.RenderSystem.disableScissor();/*ENDOLD*/
        /*MODERN*/nativeGraphics.disableScissor();/*ENDMODERN*/
    }

    /*
     * resmi verilen alana çiziyorum, blit imzası her sürümde
     * farklı olduğu için hepsi ayrı ayrı
     */
    public void blit(ResourceLocation texture, int left, int top, int right, int bottom, float u1, float u2, float v1, float v2) {
        /*OLD*/com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        net.minecraft.client.gui.GuiComponent.blit(nativeGraphics, left, top, 0, 0, right - left, bottom - top, right - left, bottom - top);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();/*ENDOLD*/
        /*MODERN*/
        /*PRE215*/
        /*PRE212*/
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        nativeGraphics.blit(texture, left, top, 0, 0, right - left, bottom - top, right - left, bottom - top);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        /*ENDPRE212*/
        /*POST212*/nativeGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, left, top, 0, 0, right - left, bottom - top, right - left, bottom - top);/*ENDPOST212*/
        /*ENDPRE215*/
        /*POST215*/
        /*PRE216*/nativeGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, left, top, 0, 0, right - left, bottom - top, right - left, bottom - top);/*ENDPRE216*/
        /*POST216*/nativeGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, left, top, 0, 0, right - left, bottom - top, right - left, bottom - top);/*ENDPOST216*/
        /*ENDPOST215*/
        /*ENDMODERN*/
    }
}
