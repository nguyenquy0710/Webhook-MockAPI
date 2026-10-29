package vn.autobot.webhook.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.service.McpRateLimiter;
import vn.autobot.webhook.service.McpService;
import vn.autobot.webhook.service.McpSessionRegistry;
import vn.autobot.webhook.service.McpTokenService;
import vn.autobot.webhook.service.SettingsService;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

/**
 * MCP server for one user, authenticated with the user's token (Authorization: Bearer mcp_...); the path only addresses
 * the user, the token must belong to that user.
 * <ul>
 *   <li>Streamable HTTP: POST /mcp/@{username}/mcp, one JSON-RPC message per request, JSON response (202 for notifications).</li>
 *   <li>Older HTTP+SSE: GET /mcp/@{username}/sse opens a stream and announces the message endpoint; answers to
 *   POST /mcp/@{username}/message?sessionId=... arrive on the stream.</li>
 * </ul>
 */
@RestController
@RequestMapping("/mcp/@{username}")
@RequiredArgsConstructor
public class McpController {

    private final McpTokenService tokenService;
    private final McpService mcpService;
    private final SettingsService settingsService;
    private final McpRateLimiter rateLimiter;
    private final McpSessionRegistry sessions;
    private final ObjectMapper mapper;

    /** Outcome of the checks every MCP request goes through: either an error response or the authenticated caller. */
    private static class Access {
        ResponseEntity<Object> error;
        User user;
        McpService.Caller caller;
    }

    @RequestMapping(value = "/mcp", method = RequestMethod.POST)
    public ResponseEntity<Object> post(@PathVariable String username, @RequestBody(required = false) String body,
            HttpServletRequest request) {
        Access access = check(username, request, true);
        if (access.error != null) {
            return access.error;
        }
        JsonNode message;
        try {
            message = parse(body);
        } catch (JsonProcessingException e) {
            return json(HttpStatus.BAD_REQUEST, mcpService.parseError());
        }
        Optional<ObjectNode> response = mcpService.handle(message, access.user, access.caller);
        return response.isPresent() ? json(HttpStatus.OK, response.get()) : ResponseEntity.accepted().build();
    }

    /** No server-initiated stream and no sessions on the Streamable HTTP endpoint: GET and DELETE are not offered. */
    @RequestMapping(value = "/mcp", method = { RequestMethod.GET, RequestMethod.DELETE })
    public ResponseEntity<Object> notAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    /** Older transport: opens the stream and tells the client where to POST its messages. */
    @RequestMapping(value = "/sse", method = RequestMethod.GET)
    public Object sse(@PathVariable String username, HttpServletRequest request) {
        Access access = check(username, request, false);
        if (access.error != null) {
            return access.error;
        }
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000);
        Optional<McpSessionRegistry.Session> session = sessions.open(access.user.getId(), emitter);
        if (!session.isPresent()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "30")
                    .body(plain("Too many open MCP streams"));
        }
        sessions.send(session.get(), "endpoint", "/mcp/@" + username + "/message?sessionId=" + session.get().id);
        return emitter;
    }

    /** Older transport: the answer is sent on the stream, the POST itself only gets 202. */
    @RequestMapping(value = "/message", method = RequestMethod.POST)
    public ResponseEntity<Object> message(@PathVariable String username, @RequestParam(required = false) String sessionId,
            @RequestBody(required = false) String body, HttpServletRequest request) {
        Access access = check(username, request, true);
        if (access.error != null) {
            return access.error;
        }
        Optional<McpSessionRegistry.Session> session = sessions.find(sessionId);
        if (!session.isPresent() || !session.get().userId.equals(access.user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(plain("Unknown or closed session"));
        }
        JsonNode message;
        try {
            message = parse(body);
        } catch (JsonProcessingException e) {
            return json(HttpStatus.BAD_REQUEST, mcpService.parseError());
        }
        Optional<ObjectNode> response = mcpService.handle(message, access.user, access.caller);
        if (response.isPresent() && !sessions.send(session.get(), "message", response.get().toString())) {
            return ResponseEntity.status(HttpStatus.GONE).body(plain("The stream is closed"));
        }
        return ResponseEntity.accepted().build();
    }

    /** MCP switched on, Origin, protocol version, token, token owner and rate limit, in that order. */
    private Access check(String username, HttpServletRequest request, boolean checkVersion) {
        Access access = new Access();
        if (!settingsService.isMcpEnabled()) {
            access.error = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(plain("MCP is turned off"));
            return access;
        }
        if (!originAllowed(request)) {
            access.error = ResponseEntity.status(HttpStatus.FORBIDDEN).body(plain("Origin not allowed"));
            return access;
        }
        String version = request.getHeader("MCP-Protocol-Version");
        if (checkVersion && version != null && !McpService.SUPPORTED_VERSIONS.contains(version)) {
            access.error = ResponseEntity.badRequest().body(plain("Unsupported MCP-Protocol-Version: " + version));
            return access;
        }
        String token = bearerToken(request);
        Optional<User> user = tokenService.authenticate(token);
        if (!user.isPresent()) {
            access.error = ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"mcp\"")
                    .body(plain("Missing or invalid token"));
            return access;
        }
        if (!user.get().getUsername().equals(username)) {
            access.error = ResponseEntity.status(HttpStatus.FORBIDDEN).body(plain("Token does not belong to this user"));
            return access;
        }
        int retryAfter = rateLimiter.tryAcquire("user:" + user.get().getId(), settingsService.getMcpRateLimitPerMinute());
        if (retryAfter > 0) {
            access.error = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
                    .body(plain("Rate limit exceeded"));
            return access;
        }
        access.user = user.get();
        access.caller = new McpService.Caller(request.getHeader(HttpHeaders.USER_AGENT), token.substring(token.length() - 4));
        return access;
    }

    private JsonNode parse(String body) throws JsonProcessingException {
        return body == null || body.trim().isEmpty() ? null : mapper.readTree(body);
    }

    private ResponseEntity<Object> json(HttpStatus status, ObjectNode body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ObjectNode plain(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return null;
    }

    /**
     * Required by the MCP spec against DNS rebinding: a browser Origin must be this host, the public base URL or one of
     * the extra origins from Admin > Settings. Requests without Origin (non-browser clients) are fine.
     */
    private boolean originAllowed(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isEmpty()) {
            return true;
        }
        try {
            String host = URI.create(origin).getHost();
            if (host == null) {
                return false;
            }
            String publicHost = URI.create(settingsService.getDomain()).getHost();
            if (host.equalsIgnoreCase(request.getServerName()) || host.equalsIgnoreCase(publicHost)) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (String extra : settingsService.getMcpExtraOrigins()) {
            if (extra.equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return false;
    }
}
