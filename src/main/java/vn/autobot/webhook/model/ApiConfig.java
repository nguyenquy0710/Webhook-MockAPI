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