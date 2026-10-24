package org.hearlov.remscompanion.api;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// java 17'de de çalışsın diye normal thread, eski mc'de virtual thread yok
final class RequestThreads {
    private RequestThreads() {}

    static Thread unstarted(Runnable task) {
        Thread thread = new Thread(task, "rem-ai-request");
        thread.setDaemon(true); // daemon, yoksa oyun kapanırken bu thread yüzünden takılıyor
        return thread;
    }

    static void start(Runnable task) { unstarted(task).start(); }
}
