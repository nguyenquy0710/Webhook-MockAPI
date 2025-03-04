package vn.autobot.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
}