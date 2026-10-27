package vn.autobot.webhook.service;

import org.springframework.stereotype.Service;
import vn.autobot.webhook.model.ApiConfig;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Lets a mock endpoint behave like a flaky service so clients can test their retry logic: for each caller the first N
 * calls get an error status (cycling through the configured codes), then the configured response is returned and the
 * counter starts over. A caller is the Idempotency-Key or X-Request-Id header, else the client IP. State lives in memory
 * and expires after 10 minutes without calls.
 */
@Service
public class FailureSimulationService {

    static final long STATE_TTL_MS = 10 * 60 * 1000L;
    private static final int MAX_STATES = 1000;
    private static final int DEFAULT_COUNT = 3;

    public static class Failure {
        public final int status;
        public final int attempt;
        public final int total;
        public final int retryAfterSeconds;

        Failure(int status, int attempt, int total, int retryAfterSeconds) {
            this.status = status;
            this.attempt = attempt;
            this.total = total;
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    private static class State {
        int failures;
        long lastSeen;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();
    LongSupplier clock = System::currentTimeMillis;

    /** @return the simulated failure for this call, or empty when the configured response should be returned */
    public Optional<Failure> next(ApiConfig config, HttpServletRequest request) {
        if (!Boolean.TRUE.equals(config.getFailureEnabled())) {
            return Optional.empty();
        }
        int total = config.getFailureCount() == null ? DEFAULT_COUNT : Math.max(1, Math.min(10, config.getFailureCount()));
        List<Integer> codes = parseCodes(config.getFailureStatusCodes());
        String key = config.getId() + "|" + callerKey(request);
        long now = clock.getAsLong();
        int[] attempt = {0};

        states.compute(key, (k, state) -> {
            if (state == null || now - state.lastSeen > STATE_TTL_MS) {
                state = new State();
            }
            state.lastSeen = now;
            if (state.failures < total) {
                attempt[0] = ++state.failures;
                return state;
            }
            return null; // all failures served: this call succeeds and the next round starts over
        });
        if (states.size() > MAX_STATES) {
            states.values().removeIf(s -> now - s.lastSeen > STATE_TTL_MS);
        }
        if (attempt[0] == 0) {
            return Optional.empty();
        }
        int retryAfter = config.getRetryAfterSeconds() == null ? 0 : Math.max(0, config.getRetryAfterSeconds());
        return Optional.of(new Failure(codes.get((attempt[0] - 1) % codes.size()), attempt[0], total, retryAfter));
    }

    /** Parses "503, 500" into error codes (400-599); falls back to 503 when nothing valid is configured. */
    static List<Integer> parseCodes(String raw) {
        List<Integer> codes = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                try {
                    int code = Integer.parseInt(part.trim());
                    if (code >= 400 && code <= 599) {
                        codes.add(code);
                    }
                } catch (NumberFormatException ignored) {
                    // skip garbage
                }
            }
        }
        if (codes.isEmpty()) {
            codes.add(503);
        }
        return codes;
    }

    private static String callerKey(HttpServletRequest request) {
        if (request == null) {
            return "anonymous";
        }
        for (String header : new String[] {"Idempotency-Key", "X-Request-Id"}) {
            String value = request.getHeader(header);
            if (value != null && !value.trim().isEmpty()) {
                return header + ":" + value.trim();
            }
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return String.valueOf(request.getRemoteAddr());
    }
}
