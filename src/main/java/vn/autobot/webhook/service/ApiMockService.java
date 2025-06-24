package vn.autobot.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.ApiConfigRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiMockService {

    private final ApiConfigRepository apiConfigRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApiConfig saveApiConfig(String username, ApiConfigDto apiConfigDto) {
        User user = userService.findByUsername(username);

        ApiConfig apiConfig;
        if (apiConfigDto.getId() != null) {
            apiConfig = apiConfigRepository.findById(apiConfigDto.getId())
                    .orElseThrow(() -> new RuntimeException("API configuration not found"));

            if (!apiConfig.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("You don't have permission to update this API configuration");
            }
        } else {
            apiConfig = new ApiConfig();
            apiConfig.setUser(user);
        }

        apiConfig.setPath(apiConfigDto.getPath());
        apiConfig.setMethod(apiConfigDto.getMethod());
        apiConfig.setContentType(apiConfigDto.getContentType());
        apiConfig.setStatusCode(apiConfigDto.getStatusCode());
        apiConfig.setResponseBody(apiConfigDto.getResponseBody());
        apiConfig.setResponseHeaders(apiConfigDto.getResponseHeaders());
        apiConfig.setDelayMs(apiConfigDto.getDelayMs());

        return apiConfigRepository.save(apiConfig);
    }

    public List<ApiConfigDto> getApiConfigs(String username) {
        User user = userService.findByUsername(username);
        List<ApiConfig> apiConfigs = apiConfigRepository.findByUser(user);

        return apiConfigs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific API configuration by ID for the given user.
     *
     * @param username The username of the user requesting the configuration.
     * @param id       The ID of the API configuration to retrieve.
     * @return The API configuration DTO.
     */
    public ApiConfigDto getApiConfig(String username, Long id) {
        User user = userService.findByUsername(username);
        ApiConfig apiConfig = apiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API configuration not found"));

        if (!apiConfig.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to view this API configuration");
        }

        return convertToDto(apiConfig);
    }

    @Transactional
    public void deleteApiConfig(String username, Long id) {
        User user = userService.findByUsername(username);
        ApiConfig apiConfig = apiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API configuration not found"));

        if (!apiConfig.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this API configuration");
        }

        apiConfigRepository.delete(apiConfig);
    }

    public Optional<ApiConfig> findApiConfig(String username, String path, String method) {
        try {
            User user = userService.findByUsername(username);
            return apiConfigRepository.findByUserAndPathAndMethod(user, path, method);
        } catch (Exception e) {
            log.error("Error finding API configuration", e);
            return Optional.empty();
        }
    }

    /**
     * Processes a webhook request asynchronously.
     *
     * @param apiConfig The API configuration to use for the response.
     * @return A DeferredResult that will be completed with the ResponseEntity.
     */
    public DeferredResult<ResponseEntity<Object>> processWebhook(ApiConfig apiConfig) {
        return processWebhook(apiConfig, null);
    }

    /**
     * Processes a webhook request asynchronously with a request context.
     *
     * @param apiConfig      The API configuration to use for the response.
     * @param requestContext Additional context for the request, can be null.
     * @return A DeferredResult that will be completed with the ResponseEntity.
     */
    public DeferredResult<ResponseEntity<Object>> processWebhook(ApiConfig apiConfig,
            Map<String, Object> requestContext) {
        if (requestContext == null) {
            requestContext = new HashMap<>();
        }

        DeferredResult<ResponseEntity<Object>> deferredResult = new DeferredResult<>();

        Integer delay = apiConfig.getDelayMs() != null ? apiConfig.getDelayMs() : 0;

        if (delay > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    deferredResult.setResult(buildResponse(apiConfig, requestContext));
                } catch (InterruptedException e) {
                    log.error("Delay interrupted", e);
                    deferredResult.setErrorResult(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Error processing webhook"));
                }
            }).start();
        } else {
            deferredResult.setResult(buildResponse(apiConfig, requestContext));
        }

        return deferredResult;
    }

    /*
     * * Builds a ResponseEntity based on the ApiConfig.
     *
     * @param apiConfig The API configuration to use for the response.
     * 
     * @return A ResponseEntity with the configured status, headers, and body.
     */
    private ResponseEntity<Object> buildResponse(ApiConfig apiConfig) {
        return buildResponse(apiConfig, null);
    }

    /**
     * Builds a ResponseEntity based on the ApiConfig and request context.
     *
     * @param apiConfig      The API configuration to use for the response.
     * @param requestContext Additional context for the request, can be null.
     * @return A ResponseEntity with the configured status, headers, and body.
     */
    private ResponseEntity<Object> buildResponse(ApiConfig apiConfig, Map<String, Object> requestContext) {
        // Nếu requestContext null, gán thành một Map rỗng để tránh lỗi
        // NullPointerException
        if (requestContext == null) {
            requestContext = new HashMap<>();
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity
                .status(apiConfig.getStatusCode() != null ? apiConfig.getStatusCode() : 200);

        // Parse and apply response headers
        if (apiConfig.getResponseHeaders() != null && !apiConfig.getResponseHeaders().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> headers = objectMapper.readValue(
                        apiConfig.getResponseHeaders(), HashMap.class);

                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    // Replace placeholders in header values
                    String value = replacePlaceholders(entry.getValue(), requestContext);
                    responseBuilder.header(entry.getKey(), value);
                    // responseBuilder.header(entry.getKey(), entry.getValue());
                }
            } catch (JsonProcessingException e) {
                log.error("Error parsing response headers", e);
            }
        }

        // Add content-type header if defined
        if (apiConfig.getContentType() != null && !apiConfig.getContentType().isEmpty()) {
            responseBuilder.header("Content-Type", apiConfig.getContentType());
        }

        // Build response body with placeholder substitution
        Object responseBody = apiConfig.getResponseBody();
        if (apiConfig.getResponseBody() != null && apiConfig.getContentType() != null
                && apiConfig.getContentType().contains("application/json")) {
            try {
                // Replace placeholders in responseBody
                String raw = apiConfig.getResponseBody();
                String replaced = replacePlaceholders(raw, requestContext);
                responseBody = objectMapper.readValue(replaced, Object.class);
            } catch (JsonProcessingException e) {
                log.warn("Could not parse JSON response body, returning as string", e);
                responseBody = apiConfig.getResponseBody();
            }
        } else if (apiConfig.getResponseBody() != null) {
            // Replace placeholders for non-JSON response body
            responseBody = replacePlaceholders(apiConfig.getResponseBody(), requestContext);
        }

        return responseBuilder.body(responseBody);
    }

    private ApiConfigDto convertToDto(ApiConfig apiConfig) {
        ApiConfigDto dto = new ApiConfigDto();
        dto.setId(apiConfig.getId());
        dto.setPath(apiConfig.getPath());
        dto.setMethod(apiConfig.getMethod());
        dto.setContentType(apiConfig.getContentType());
        dto.setStatusCode(apiConfig.getStatusCode());
        dto.setResponseBody(apiConfig.getResponseBody());
        dto.setResponseHeaders(apiConfig.getResponseHeaders());
        dto.setDelayMs(apiConfig.getDelayMs());
        return dto;
    }

    private String replacePlaceholders(String raw, Map<String, Object> requestContext) {
        // Check if the context is null or empty
        if (requestContext == null || requestContext.isEmpty()) {
            return raw;
        }

        // Loop through each placeholder and replace it
        for (Map.Entry<String, Object> entry : requestContext.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}"; // Format: {{key}}
            Object value = entry.getValue();

            // Convert value to string if it's not null
            if (value != null) {
                String valueStr;
                try {
                    valueStr = value instanceof Map ? objectMapper.writeValueAsString(value) : value.toString();
                } catch (JsonProcessingException e) {
                    log.error("Error serializing value for placeholder: {}", entry.getKey(), e);
                    valueStr = value.toString(); // fallback to .toString() if serialization fails
                }
                raw = raw.replace(placeholder, valueStr);
            } else {
                // If value is null, replace the placeholder with empty string or handle
                // accordingly
                raw = raw.replace(placeholder, "");
            }
        }
        return raw;
    }

    private Object resolveContextValue(String key, Map<String, Object> context) {
        if (context == null)
            return null; // Nếu context là null, trả về null ngay lập tức.

        // Kiểm tra nếu key có dạng "header.User-Agent" hoặc "name"
        if (key.contains(".")) {
            String[] parts = key.split("\\.");
            if (parts.length == 2) {
                Object parent = context.get(parts[0]);
                if (parent instanceof Map<?, ?> map) {
                    return map.get(parts[1]);
                }
            }
        } else {
            return context.get(key);
        }
        return null;
    }
}