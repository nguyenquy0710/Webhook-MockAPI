package vn.autobot.webhook.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String method;  // GET, POST, PUT, DELETE, etc.

    @Column(name = "content_type")
    private String contentType;  // application/json, text/plain, etc.

    @Column(name = "status_code")
    private Integer statusCode;  // 200, 201, 400, 404, etc.

    @Column(name = "response_body")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String responseBody;

    @Column(name = "response_headers")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String responseHeaders;  // Stored as JSON

    @Column(name = "delay_ms")
    private Integer delayMs;  // Delay in milliseconds before responding

    @Column(name = "description", length = 500)
    private String description;  // Short purpose of the endpoint

    @Column(name = "note", length = 2000)
    private String note;  // Free-form note for testers

    // Failure simulation: fail the first N calls of each caller with these status codes, then respond normally
    @Column(name = "failure_enabled")
    private Boolean failureEnabled = false;

    @Column(name = "failure_count")
    private Integer failureCount = 3;

    @Column(name = "failure_status_codes", length = 100)
    private String failureStatusCodes = "503";  // comma-separated, 400-599

    @Column(name = "retry_after_seconds")
    private Integer retryAfterSeconds = 1;  // sent as Retry-After on 429/503 failures; 0 omits it

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}