package vn.autobot.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralSettingsDto {

    @NotBlank(message = "Site name is required")
    @Size(max = 60, message = "Site name must be at most 60 characters")
    private String siteName;

    // Empty means "use app.domain from the configuration file"
    @Size(max = 200, message = "Public base URL must be at most 200 characters")
    @Pattern(regexp = "^$|^https?://[^\\s/]+[^\\s]*$", message = "Public base URL must start with http:// or https://")
    private String publicBaseUrl;

    private boolean allowRegistration;

    // 0 keeps every request log; otherwise logs older than this many days are deleted by the daily maintenance run
    @Min(value = 0, message = "Retention must be 0 or more days")
    @Max(value = 3650, message = "Retention must be at most 3650 days")
    private int requestLogRetentionDays;
}
