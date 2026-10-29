package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.autobot.webhook.model.McpToken;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.McpTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/** One MCP access token per user. Renewing replaces the token, so the old one stops working immediately. */
@Service
@RequiredArgsConstructor
public class McpTokenService {

    public static final String PREFIX = "mcp_";
    private static final Duration LAST_USED_THROTTLE = Duration.ofMinutes(1);

    private final McpTokenRepository repository;
    private final UserService userService;
    private final SecureRandom random = new SecureRandom();
    Clock clock = Clock.systemUTC();

    /** Creates the user's token or replaces the existing one. @return the new token; it cannot be read again. */
    @Transactional
    public String renew(String username) {
        User user = userService.findByUsername(username);
        String token = generate();
        McpToken entity = repository.findByUser(user).orElseGet(McpToken::new);
        entity.setUser(user);
        entity.setTokenHash(hash(token));
        entity.setTokenSuffix(token.substring(token.length() - 4));
        entity.setCreatedAt(LocalDateTime.now(clock));
        entity.setLastUsedAt(null);
        repository.save(entity);
        return token;
    }

    /** @return the owner of the token, or empty when the token is missing, malformed or no longer valid */
    @Transactional
    public Optional<User> authenticate(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return Optional.empty();
        }
        Optional<McpToken> found = repository.findByTokenHash(hash(token));
        found.ifPresent(t -> {
            LocalDateTime now = LocalDateTime.now(clock);
            // Throttled so a busy client does not write on every call
            if (t.getLastUsedAt() == null || Duration.between(t.getLastUsedAt(), now).compareTo(LAST_USED_THROTTLE) >= 0) {
                t.setLastUsedAt(now);
                repository.save(t);
            }
        });
        return found.map(McpToken::getUser);
    }

    public Optional<McpToken> find(String username) {
        return repository.findByUser(userService.findByUsername(username));
    }

    public long countTokens() {
        return repository.count();
    }

    private String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
