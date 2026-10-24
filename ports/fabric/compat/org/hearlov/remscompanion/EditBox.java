package org.hearlov.remscompanion;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
/** Keeps the same field API on 1.18 and later. */
class EditBox extends net.minecraft.client.gui.components.EditBox {
    /*
     * vanilla yazı kutusunu aynı imzayla açıyorum,
     * font ve konum olduğu gibi üst sınıfa gidiyor
     */
    EditBox(Font font, int x, int y, int width, int height, Component label) { super(font, x, y, width, height, label); }
    /*
     * 1.18'de setHint yok, ipucunu Tip ile tooltip gibi gösteriyorum
     * (bu satır sadece eski sürümde derleniyor)
     */
    /*OLD*/public void setHint(Component hint) { org.hearlov.remscompanion.compat.Tip.set(this, hint); }/*ENDOLD*/
}
