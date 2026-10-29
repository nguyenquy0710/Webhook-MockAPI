package vn.autobot.webhook.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/** One MCP tool call made with a user's token, kept so the user can see what an AI client did for them. */
@Entity
@Table(name = "mcp_calls")
@Getter
@Setter
@NoArgsConstructor
public class McpCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String tool;

    @Column(length = 2000)
    private String arguments;  // redacted, truncated JSON

    @Column(name = "result_ok", nullable = false)
    private boolean resultOk;

    @Column(name = "response_excerpt", length = 2000)
    private String responseExcerpt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "client_name", length = 100)
    private String clientName;

    @Column(name = "token_suffix", length = 8)
    private String tokenSuffix;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
