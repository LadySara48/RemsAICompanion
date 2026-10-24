package org.hearlov.remscompanion.compat;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public record KeyEvent(int key, int scanCode, int modifiers) {
    /*
     * shift basılı mı, modifier bitinden bakıyorum
     */
    public boolean hasShiftDown() { return (modifiers & 1) != 0; }
}
