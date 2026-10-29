package vn.autobot.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiConfigDto {

    private Long id;

    @NotEmpty(message = "Path cannot be empty")
    private String path;

    @NotEmpty(message = "Method cannot be empty")
    private String method;

    @NotEmpty(message = "Content type cannot be empty")
    private String contentType;

    @NotNull(message = "Status code cannot be empty")
    @Min(value = 100, message = "Status code must be at least 100")
    @Max(value = 599, message = "Status code must be at most 599")
    private Integer statusCode;

    private String responseBody;

    private String responseHeaders;

    @Min(value = 0, message = "Delay cannot be negative")
    @Max(value = 60000, message = "Delay cannot be more than 60 seconds")
    private Integer delayMs;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @Size(max = 2000, message = "Note must be at most 2000 characters")
    private String note;

    private Boolean failureEnabled;

    @Min(value = 1, message = "Failures must be at least 1")
    @Max(value = 10, message = "Failures must be at most 10")
    private Integer failureCount;

    @Size(max = 100, message = "Failure status codes must be at most 100 characters")
    @Pattern(regexp = "^$|^[45]\\d\\d(\\s*,\\s*[45]\\d\\d)*$", message = "Use comma-separated status codes between 400 and 599")
    private String failureStatusCodes;

    @Min(value = 0, message = "Retry-After cannot be negative")
    @Max(value = 60, message = "Retry-After must be at most 60 seconds")
    private Integer retryAfterSeconds;

    // Read-only: ISO timestamp of the last change, used by MCP clients to avoid overwriting newer edits
    private String updatedAt;
}