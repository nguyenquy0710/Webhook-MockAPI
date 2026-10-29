package vn.autobot.webhook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Open SSE streams of the older MCP transport (GET /sse, answers come back on the stream after POST /message).
 * In memory: sessions are lost on restart and are not shared between instances, clients simply reconnect.
 */
@Component
@Slf4j
public class McpSessionRegistry {

    static final int MAX_PER_USER = 5;
    static final int MAX_TOTAL = 200;

    public static class Session {
        public final String id = UUID.randomUUID().toString();
        public final Long userId;
        final SseEmitter emitter;

        Session(Long userId, SseEmitter emitter) {
            this.userId = userId;
            this.emitter = emitter;
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** @return the new session, or empty when the user or the server has too many open streams */
    public synchronized Optional<Session> open(Long userId, SseEmitter emitter) {
        long mine = sessions.values().stream().filter(s -> java.util.Objects.equals(s.userId, userId)).count();
        if (mine >= MAX_PER_USER || sessions.size() >= MAX_TOTAL) {
            return Optional.empty();
        }
        Session session = new Session(userId, emitter);
        sessions.put(session.id, session);
        Runnable cleanup = () -> sessions.remove(session.id);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        return Optional.of(session);
    }

    public Optional<Session> find(String id) {
        return Optional.ofNullable(id == null ? null : sessions.get(id));
    }

    public int size() {
        return sessions.size();
    }

    /** @return false when the stream is gone (the session is dropped) */
    public boolean send(Session session, String event, String data) {
        try {
            session.emitter.send(SseEmitter.event().name(event).data(data));
            return true;
        } catch (IOException | IllegalStateException e) {
            sessions.remove(session.id);
            return false;
        }
    }

    /** Keeps idle streams alive through proxies and drops the dead ones. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        for (Session s : sessions.values()) {
            try {
                s.emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                sessions.remove(s.id);
            }
        }
    }
}
