package vn.autobot.webhook.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestLogDto {

    private Long id;
    private String method;
    private String path;
    private String sourceIp;
    private String queryParams;
    private String requestHeaders;
    private String requestBody;
    private LocalDateTime timestamp;
    private Integer responseStatus;
    private String responseBody;
    private String curl;}