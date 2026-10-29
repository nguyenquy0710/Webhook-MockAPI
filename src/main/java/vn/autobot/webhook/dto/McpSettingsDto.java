package vn.autobot.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpSettingsDto {

    private boolean enabled;

    @Min(value = 1, message = "Rate limit must be between 1 and 600")
    @Max(value = 600, message = "Rate limit must be between 1 and 600")
    private int rateLimitPerMinute;

    @Min(value = 1, message = "Page size must be between 1 and 200")
    @Max(value = 200, message = "Page size must be between 1 and 200")
    private int maxPageSize;

    // One origin per line, e.g. http://localhost:6274
    @Size(max = 1000, message = "Extra origins must be at most 1000 characters")
    private String extraOrigins;
}
