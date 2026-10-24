package org.hearlov.remscompanion;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hearlov.remscompanion.api.*;
import org.hearlov.remscompanion.storage.*;
import org.hearlov.remscompanion.api.fabric.FabricChatStorage;
import java.util.UUID;
import java.util.concurrent.*;
import java.io.IOException;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
/** UI state stays on Minecraft's client thread. */
final class ChatSession {
    static final List<ChatSession> SESSIONS = new ArrayList<>();
    static ChatSession selected;
    private static final ChatStore STORE = FabricChatStorage.create();
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rem-chat-storage"); t.setDaemon(true); return t;
    });
    private static boolean loaded, loadFailed;
    static boolean unreadableChats;
    static volatile boolean saveFailed;
    private static volatile boolean indexSaveFailed;
    final int number;
    final UUID id;
    final long createdAt;
    private long updatedAt, lastSaved;
    private boolean dirty;
    private volatile boolean writeFailed;
    static final class Message extends ChatRecord.Message {
        /*
         * Kayıttaki mesaj yapısını aynen kullanan küçük bir kurucu.
         */
        Message(String role, String text, boolean complete) { super(role, text, complete); }
    }
    final List<Message> messages = new ArrayList<>();
    final Set<String> sources = new LinkedHashSet<>();
    boolean busy;
    String error = "";
    AiClient.Call call;
    String draft = "";
    String title = "";
    boolean manualTitle, autoTitleSet;
    boolean pinned, deleted;
    final List<String> heldDrafts = new ArrayList<>();
    String profileId = "", promptId = "", summary = "", summaryDraft = "";
    int summaryThrough;
    boolean summaryBusy;

    /*
     * Yeni ve boş bir sohbet oluşturur, numara ve rastgele id verir.
     */
    private ChatSession(int number) {
        this.number = number; this.id = UUID.randomUUID();
        createdAt = System.currentTimeMillis(); updatedAt = createdAt;
    }

    /*
     * Diskten okunan kayıttan sohbeti geri kurar.
     * Yarım kalan cevabı toparlar, başlık yoksa ilk cevaptan çıkarır.
     */
    private ChatSession(ChatRecord data) {
        number = data.number; id = data.id; createdAt = data.createdAt; updatedAt = data.updatedAt;
        if (data.recoverInterrupted()) error = "interrupted_chat";
        draft = data.draft; sources.addAll(data.sources);
        title = data.title; manualTitle = data.manualTitle; autoTitleSet = data.autoTitleSet;
        pinned = data.pinned; heldDrafts.addAll(data.heldDrafts);
        profileId = data.profileId; promptId = data.promptId; summary = data.summary; summaryThrough = data.summaryThrough;
        for (ChatRecord.Message message : data.messages) {
            Message copy = new Message(message.role(), message.text(), message.complete());
            copy.alternatives.addAll(message.alternatives); copy.alternativeSources.addAll(message.alternativeSources);
            copy.variant = message.variant; copy.regenerations = message.regenerations; messages.add(copy);
        }
        if (!manualTitle && !autoTitleSet) {
            for (Message message : messages) if (message.role.equals("assistant") && message.complete) { autoTitle(message.text); break; }
        }
    }

    /*
     * Kayıtlı sohbetleri bir kere yükler ve en son seçili olanı seçer.
     */
    static void load() {
        if (loaded) return;
        loaded = true;
        try {
            ChatStore.Loaded data = STORE.load();
            unreadableChats = data.unreadableFiles() > 0;
            for (ChatRecord chat : data.chats()) SESSIONS.add(new ChatSession(chat));
            selected = SESSIONS.stream().filter(s -> s.id.equals(data.selected())).findFirst().orElse(SESSIONS.isEmpty() ? null : SESSIONS.getLast());
        } catch (IOException ex) { loadFailed = true; unreadableChats = true; }
    }

    /*
     * Sohbeti değişti diye işaretler ki birazdan kaydedilsin.
     */
    void changed() { dirty = true; updatedAt = System.currentTimeMillis(); }
    /*
     * Kullanıcının bu sohbette kaç mesaj yazdığını sayar.
     */
    long userMessages() { return messages.stream().filter(m -> m.role.equals("user")).count(); }
    /*
     * Mesaj sınırına ulaşıldı mı, onu söyler.
     */
    boolean atLimit() { return userMessages() >= ChatStore.MAX_USER_MESSAGES; }
    /*
     * Sohbete bağlı API profilini bulur, yoksa aktif olanı verir.
     */
    AiProfile profile() { return Connections.store.resolve(profileId); }
    /*
     * Sohbetin kullanacağı prompt id'sini bulur, silinmişse default'a döner.
     */
    String resolvedPrompt() {
        String id = promptId.isBlank() ? Prompts.store.selected : promptId;
        return id.equals("default") || Prompts.store.prompts.stream().anyMatch(p -> p.id.equals(id)) ? id : "default";
    }

    /*
     * Şu anki profil ve promptu bu sohbete sabitler.
     */
    void bind() { profileId = profile().id; promptId = resolvedPrompt(); changed(); }
    /*
     * Sohbete elle isim verir, boş bırakılırsa otomatik başlığa izin verir.
     */
    void rename(String value) { title = ChatTitles.normalize(value); manualTitle = !title.isBlank(); changed(); save(); }
    /*
     * Elle isim yoksa ilk cevaptan kısa bir başlık üretir, sadece bir kere.
     */
    void autoTitle(String answer) {
        if (manualTitle || autoTitleSet) return;
        title = ChatTitles.fromAnswer(answer); autoTitleSet = true; changed();
    }

    /*
     * Sohbetin o anki halini kaydedilebilir bir ChatRecord kopyasına çevirir.
     */
    private ChatRecord snapshot() {
        ChatRecord data = new ChatRecord();
        data.id = id; data.number = number; data.createdAt = createdAt; data.updatedAt = updatedAt;
        data.draft = draft; data.inProgress = busy && !summaryBusy; data.sources.addAll(sources);
        data.title = title; data.manualTitle = manualTitle; data.autoTitleSet = autoTitleSet;
        data.pinned = pinned; data.heldDrafts.addAll(heldDrafts);
        data.profileId = profileId; data.promptId = promptId; data.summary = summary; data.summaryThrough = summaryThrough;
        for (Message message : messages) {
            var copy = new ChatRecord.Message(message.role, message.text, message.complete);
            copy.alternatives.addAll(message.alternatives);
            for (var sourceList : message.alternativeSources) copy.alternativeSources.add(new ArrayList<>(sourceList));
            copy.variant = message.variant; copy.regenerations = message.regenerations; data.messages.add(copy);
        }
        return data;
    }

    /*
     * Sohbeti arka plandaki yazıcı thread'inde diske kaydeder.
     */
    void save() {
        if (deleted || loadFailed || WRITER.isShutdown()) return;
        ChatRecord data = snapshot(); dirty = false; lastSaved = System.nanoTime();
        WRITER.execute(() -> {
            try { STORE.save(data); writeFailed = false; }
            catch (IOException ex) { writeFailed = true; }
        });
    }

    /*
     * Sohbeti seçili yapar ve bu seçimi arka planda index dosyasına yazar.
     */
    static void select(ChatSession session) {
        selected = session;
        if (!loadFailed && !WRITER.isShutdown()) WRITER.execute(() -> {
            try { STORE.select(session.id); indexSaveFailed = false; }
            catch (IOException ex) { indexSaveFailed = true; }
        });
    }

    /*
     * Sohbetleri önce sabitlenenler gelecek şekilde sıralar.
     */
    static void sort() {
        SESSIONS.sort(java.util.Comparator.comparing((ChatSession s) -> !s.pinned).thenComparingInt(s -> s.number));
    }

    /*
     * Sohbeti siler: isteği iptal eder, dosyayı arka planda silip
     * sonucu oyun thread'inde geri bildirir.
     */
    static void delete(ChatSession session, java.util.function.Consumer<Boolean> completed) {
        if (loadFailed) { completed.accept(false); return; }
        session.deleted = true;
        if (session.call != null) session.call.cancel();
        if (session.busy && !session.summaryBusy && session.messages.size() >= 2) session.messages.get(session.messages.size() - 2).complete = false;
        session.busy = false; session.call = null;
        WRITER.execute(() -> {
            boolean success;
            try { STORE.delete(session.id); success = true; }
            catch (IOException ex) { success = false; }
            boolean result = success;
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                if (result) {
                    SESSIONS.remove(session);
                    if (selected == session) {
                        if (SESSIONS.isEmpty()) create(); else select(SESSIONS.getFirst());
                    }
                } else { session.deleted = false; session.error = "chat_delete_failed"; }
                completed.accept(result);
            });
        });
    }

    /*
     * Değişen sohbetleri saniyede bir kaydeder,
     * yazılamayanları da birkaç saniye sonra tekrar dener.
     */
    static void tick() {
        for (ChatSession session : SESSIONS) {
            if (session.writeFailed && System.nanoTime() - session.lastSaved >= 5_000_000_000L) session.dirty = true;
            if (session.dirty && System.nanoTime() - session.lastSaved >= 1_000_000_000L) session.save();
        }
        saveFailed = indexSaveFailed || SESSIONS.stream().anyMatch(session -> session.writeFailed);
    }

    /*
     * Kaydedilmemiş ne varsa hemen kayda gönderir.
     */
    static void flush() {
        for (ChatSession session : SESSIONS) if (session.dirty || session.busy || session.writeFailed) session.save();
    }

    /*
     * Oyun kapanırken her şeyi kaydeder, istekleri iptal eder, yedek alır
     * ve yazıcıyı en fazla 5 saniye bekler.
     */
    static void shutdown() {
        flush();
        for (ChatSession session : SESSIONS) if (session.call != null) session.call.cancel();
        enqueue(() -> { try { BackupFiles.mirror(AppState.ROOT); } catch (IOException ex) { RemsAICompanionClient.LOGGER.warn("Backup could not be completed."); } });
        WRITER.shutdown();
        try { if (!WRITER.awaitTermination(5, TimeUnit.SECONDS)) RemsAICompanionClient.LOGGER.warn("Chat history is still being saved during shutdown."); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }

    /*
     * Yazıcı kapanmadıysa bir işi kayıt kuyruğuna ekler.
     */
    static void enqueue(Runnable operation) { if (!WRITER.isShutdown()) WRITER.execute(operation); }

    /*
     * Yeni sohbet açar, listeye ekler, seçer ve hemen kaydeder.
     */
    static ChatSession create() {
        load();
        ChatSession session = new ChatSession(SESSIONS.stream().mapToInt(s -> s.number).max().orElse(0) + 1);
        SESSIONS.add(session); sort(); select(session); session.save();
        return selected;
    }
}
