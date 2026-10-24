package org.hearlov.remscompanion.compat;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class Numbers {
    /*
     * int değeri min ile max arasına sıkıştırıyor, Math.clamp her java'da yok
     */
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, max)); }
    /*
     * aynısının double hali
     */
    public static double clamp(double value, double min, double max) { return Math.max(min, Math.min(value, max)); }
}
