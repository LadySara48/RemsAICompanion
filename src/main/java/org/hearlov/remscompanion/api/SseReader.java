package org.hearlov.remscompanion.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class SseReader {
    private SseReader() {}

    public static void read(Reader source, Consumer<String> event) throws IOException {
        read(source, event, () -> false);
    }

    public static void read(Reader source, Consumer<String> event, BooleanSupplier finished) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (!data.isEmpty()) { event.accept(data.toString()); data.setLength(0); }
                if (finished.getAsBoolean()) return;
            } else if (line.startsWith("data:")) {
                if (!data.isEmpty()) data.append('\n'); // çok satırlı data, araya satır sonu koyup birleştiriyorum
                String value = line.substring(5);
                data.append(value.startsWith(" ") ? value.substring(1) : value);
                if (data.length() > 4_000_000) throw new IOException("Stream event too large"); // tek event çok büyükse kes
            }
        }

        if (!data.isEmpty()) event.accept(data.toString());
    }
}
