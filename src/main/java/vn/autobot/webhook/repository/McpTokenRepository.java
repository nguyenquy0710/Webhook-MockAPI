package vn.autobot.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.autobot.webhook.model.McpToken;
import vn.autobot.webhook.model.User;

import java.util.Optional;

public interface McpTokenRepository extends JpaRepository<McpToken, Long> {

    Optional<McpToken> findByUser(User user);

    Optional<McpToken> findByTokenHash(String tokenHash);
}
