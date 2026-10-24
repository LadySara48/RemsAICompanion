package org.hearlov.remscompanion.companion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// saat ve random dışarıdan geliyor, olasılıkları ve cooldownları test edebileyim diye
public final class CompanionBehaviour {
    private final LongSupplier clock;
    private final DoubleSupplier random;
    private final Consumer<String> voice;
    private final Deque<Long> answers = new ArrayDeque<>();
    private long lastInput, nextRareCheck, nextHehe, nextComplaint;
    private boolean typed, sleepPlayed;

    public CompanionBehaviour(LongSupplier clock, DoubleSupplier random, Consumer<String> voice) {
        this.clock = clock; this.random = random; this.voice = voice; lastInput = clock.getAsLong();
    }

    public void input(boolean inserted) {
        long now = clock.getAsLong(); lastInput = now; typed = true; sleepPlayed = false;

        if (inserted && now >= nextRareCheck) {
            nextRareCheck = now + 30_000; // her tuşta zar atmasın, 30 sn'de bi şans
            if (now >= nextHehe && random.getAsDouble() < 0.01) { voice.accept("hehe"); nextHehe = now + 120_000; }
        }
    }

    public void sent() { lastInput = clock.getAsLong(); typed = false; sleepPlayed = false; voice.accept("start_thinking"); }
    public void blockedSend() {
        long now = clock.getAsLong();
        if (now >= nextComplaint) { voice.accept("still_thinking"); nextComplaint = now + 8_000; }
    }

    public void answered() {
        long now = clock.getAsLong(); prune(now);
        boolean previouslyHeart = answers.size() >= 5;
        answers.addLast(now);
        if (!previouslyHeart && answers.size() >= 5 && random.getAsDouble() < 0.75) voice.accept("rem_heart");
        if (random.getAsDouble() < 0.10) voice.accept("got_answer");
    }

    public void tick(boolean busy, boolean visible) {
        if (visible && !busy && clock.getAsLong() - lastInput >= 900_000 && !sleepPlayed) { // 15 dk boş kalınca uyuma sesi, sadece bi kere
            sleepPlayed = true; voice.accept("sleep");
        }
    }

    public String image(boolean busy, boolean streaming, boolean hasDraft) {
        long now = clock.getAsLong(); prune(now);
        if (busy) return streaming ? ((now / 200) % 2 == 0 ? "rem_typing_1" : "rem_typing_2") : "rem_thinking";
        if (now - lastInput >= 900_000) return "rem_sleep";
        if (typed && hasDraft && now - lastInput < 5_000) return "rem_question";
        return answers.size() >= 5 ? "rem_heart" : "rem_idle";
    }

    private void prune(long now) { while (!answers.isEmpty() && now - answers.peekFirst() >= 300_000) answers.removeFirst(); }
}
