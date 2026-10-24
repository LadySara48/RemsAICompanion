package org.hearlov.remscompanion.settings;

import org.hearlov.remscompanion.api.AiUsage;
import java.time.LocalDate;
import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class UsageLedger {
    public static final class Totals {
        public long input, output, tokens, requests, unreported, costReports;
        public double usd;
        public void add(Totals value) { input += value.input; output += value.output; tokens += value.tokens;
            requests += value.requests; unreported += value.unreported; costReports += value.costReports; usd += value.usd; }
    }

    public Totals lifetime = new Totals();
    public TreeMap<String, Totals> days = new TreeMap<>();

    public void record(LocalDate date, AiUsage usage) {
        Totals entry = new Totals(); entry.requests = 1; entry.input = usage.input; entry.output = usage.output;
        entry.tokens = usage.total; entry.unreported = usage.reported ? 0 : 1; // sağlayıcı kullanım yollamadıysa ayrıca say
        entry.usd = usage.costReported ? usage.reportedUsd : 0;
        entry.costReports = usage.costReported ? 1 : 0;
        lifetime.add(entry); days.computeIfAbsent(date.toString(), k -> new Totals()).add(entry); prune(date);
    }

    public void prune(LocalDate today) { days.headMap(today.minusDays(30).toString(), false).clear(); while (days.size() > 31) days.pollFirstEntry(); } // son 30 gün yeter, dosya şişmesin
    public Totals since(LocalDate start, LocalDate today) {
        Totals totals = new Totals();
        days.subMap(start.toString(), true, today.toString(), true).values().forEach(totals::add); return totals;
    }

    public void validate() throws java.io.IOException {
        if (lifetime == null || days == null) throw new java.io.IOException("Invalid usage ledger");
        validate(lifetime);
        for (var entry : days.entrySet()) { LocalDate.parse(entry.getKey()); validate(entry.getValue()); }
    }

    private static void validate(Totals t) throws java.io.IOException {
        if (t == null || t.input < 0 || t.output < 0 || t.tokens < 0 || t.requests < 0 || t.unreported < 0
                || t.costReports < 0 || !Double.isFinite(t.usd) || t.usd < 0) throw new java.io.IOException("Invalid totals");
    }
}
