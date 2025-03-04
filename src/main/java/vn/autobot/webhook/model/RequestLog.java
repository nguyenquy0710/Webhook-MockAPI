package vn.autobot.webhook.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String path;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "query_params")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String queryParams;

    @Column(name = "request_headers")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String requestHeaders;

    @Column(name = "request_body")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String requestBody;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String responseBody;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}