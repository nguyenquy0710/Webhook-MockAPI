package vn.autobot.webhook.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/** Bearer token that lets an MCP client act as its owner; only the hash of the token is stored. */
@Entity
@Table(name = "mcp_tokens")
@Getter
@Setter
@NoArgsConstructor
public class McpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Last characters of the token, so the dashboard can tell tokens apart without storing the secret
    @Column(name = "token_suffix", length = 8)
    private String tokenSuffix;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
