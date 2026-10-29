package vn.autobot.webhook.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window limiter: at most N MCP requests per token per minute (in memory, per instance). */
@Component
public class McpRateLimiter {

    private static class Window {
        long minute;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    Clock clock = Clock.systemUTC();

    /** @return 0 when the request is allowed, otherwise the seconds until the window resets */
    public int tryAcquire(String key, int limit) {
        long now = clock.millis();
        long minute = now / 60_000;
        int[] retryAfter = {0};
        windows.compute(key, (k, w) -> {
            if (w == null || w.minute != minute) {
                w = new Window();
                w.minute = minute;
            }
            if (w.count >= limit) {
                retryAfter[0] = (int) Math.max(1, ((w.minute + 1) * 60_000 - now + 999) / 1000);
            } else {
                w.count++;
            }
            return w;
        });
        if (windows.size() > 5000) {
            windows.values().removeIf(w -> w.minute < minute);
        }
        return retryAfter[0];
    }
}
