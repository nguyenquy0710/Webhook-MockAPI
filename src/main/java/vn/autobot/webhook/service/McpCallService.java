package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.autobot.webhook.model.McpCall;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.McpCallRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Audit trail of MCP tool calls (what an AI client did with a user's token). */
@Service
@RequiredArgsConstructor
public class McpCallService {

    /** Tools that change data; shown as "write" in the activity page. */
    public static final List<String> WRITE_TOOLS = Arrays.asList("create_api_config", "update_api_config");
    static final int RETENTION_DAYS = 90;

    private final McpCallRepository repository;
    private final UserService userService;

    public static class Stats {
        public final long callsToday;
        public final long errorsToday;
        public final long changesToday;

        Stats(long callsToday, long errorsToday, long changesToday) {
            this.callsToday = callsToday;
            this.errorsToday = errorsToday;
            this.changesToday = changesToday;
        }

        public long getCallsToday() { return callsToday; }
        public long getErrorsToday() { return errorsToday; }
        public long getChangesToday() { return changesToday; }
    }

    @Transactional
    public void record(User user, String tool, JsonNode arguments, boolean ok, String response, long durationMs,
            McpService.Caller caller) {
        McpCall call = new McpCall();
        call.setUser(user);
        call.setTool(McpRedactor.truncate(tool, 100));
        call.setArguments(McpRedactor.truncate(arguments == null || arguments.isMissingNode() ? "{}"
                : McpRedactor.redact(arguments).toString(), 2000));
        call.setResultOk(ok);
        call.setResponseExcerpt(McpRedactor.truncate(response, 2000));
        call.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
        call.setClientName(McpRedactor.truncate(caller.client, 100));
        call.setTokenSuffix(caller.tokenSuffix);
        call.setCreatedAt(LocalDateTime.now());
        repository.save(call);
    }

    public Page<McpCall> search(String username, String tool, String result, String q, Pageable pageable) {
        User user = userService.findByUsername(username);
        int r = "ok".equals(result) ? 1 : "error".equals(result) ? 2 : 0;
        return repository.search(user, tool == null ? "" : tool.trim(), r, q == null ? "" : q.trim(), pageable);
    }

    public Stats statsToday(String username) {
        User user = userService.findByUsername(username);
        LocalDateTime since = LocalDate.now().atStartOfDay();
        return new Stats(repository.countByUserAndCreatedAtGreaterThanEqual(user, since),
                repository.countByUserAndResultOkFalseAndCreatedAtGreaterThanEqual(user, since),
                repository.countByUserAndResultOkTrueAndToolInAndCreatedAtGreaterThanEqual(user, WRITE_TOOLS, since));
    }

    public long callsLast24h() {
        return repository.countByCreatedAtGreaterThanEqual(LocalDateTime.now().minusHours(24));
    }

    public long errorsLast24h() {
        return repository.countByResultOkFalseAndCreatedAtGreaterThanEqual(LocalDateTime.now().minusHours(24));
    }

    /** Called by the maintenance run so the table does not grow forever. */
    @Transactional
    public int purgeOld() {
        return repository.deleteOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
    }
}
