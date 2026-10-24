package org.hearlov.remscompanion.settings;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class PerformanceSettings {
    public enum Context {
        ECONOMIC("Economic", 16_000), STABLE("Stable", 24_000), EXTENDED("Extended", 32_000), ULTRA("Ultra", 64_000);
        public final String label; public final int characters;
        Context(String label, int characters) { this.label = label; this.characters = characters; }
    }

    public Context context = Context.ECONOMIC; // ilk açılışta economic, en hafifi bu
}
