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
import vn.autobot.webhook.utils.RequestUtils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

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

  /**
   * Converts ApiConfig to ApiConfigDto.
   *
   * @param apiConfig The API configuration to convert.
   * @return The converted API configuration DTO.
   */
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

  /**
   * Converts ApiConfig to ApiConfigDto.
   *
   * @param apiConfig The API configuration to convert.
   * @return The converted API configuration DTO.
   */
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
   * Processes a webhook request based on the provided ApiConfig.
   *
   * @param apiConfig The API configuration to process.
   * @return A DeferredResult containing the response entity.
   */
  // public DeferredResult<ResponseEntity<Object>> processWebhook(ApiConfig
  // apiConfig) {
  // return processWebhook(apiConfig, null);
  // }

  /**
   * Processes a webhook request based on the provided ApiConfig and
   * HttpServletRequest.
   *
   * @param apiConfig The API configuration to process.
   * @param request   The HttpServletRequest to use for context.
   * @return A DeferredResult containing the response entity.
   */
  public DeferredResult<ResponseEntity<Object>> processWebhook(ApiConfig apiConfig, HttpServletRequest request) {
    DeferredResult<ResponseEntity<Object>> deferredResult = new DeferredResult<>();

    Integer delay = apiConfig.getDelayMs() != null ? apiConfig.getDelayMs() : 0;

    if (delay > 0) {
      new Thread(() -> {
        try {
          Thread.sleep(delay);
          deferredResult.setResult(buildResponse(apiConfig, request));
        } catch (InterruptedException e) {
          log.error("Delay interrupted", e);
          deferredResult.setErrorResult(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body("Error processing webhook"));
        }
      }).start();
    } else {
      deferredResult.setResult(buildResponse(apiConfig, request));
    }

    return deferredResult;
  }

  /**
   * Builds a ResponseEntity based on the ApiConfig.
   *
   * @param apiConfig The API configuration to build the response from.
   * @return A ResponseEntity containing the response body and headers.
   */
  // private ResponseEntity<Object> buildResponse(ApiConfig apiConfig) {
  // return buildResponse(apiConfig, null);
  // }

  /**
   * Builds a ResponseEntity based on the ApiConfig and HttpServletRequest.
   *
   * @param apiConfig The API configuration to build the response from.
   * @param request   The HttpServletRequest to use for context.
   * @return A ResponseEntity containing the response body and headers.
   */
  private ResponseEntity<Object> buildResponse(ApiConfig apiConfig, HttpServletRequest request) {
    int statusCode = apiConfig.getStatusCode() != null ? apiConfig.getStatusCode() : 200;
    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity
        .status(apiConfig.getStatusCode() != null ? apiConfig.getStatusCode() : 200);

    // Parse response headers
    if (apiConfig.getResponseHeaders() != null && !apiConfig.getResponseHeaders().isEmpty()) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, String> headers = objectMapper.readValue(
            apiConfig.getResponseHeaders(), HashMap.class);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
          responseBuilder.header(entry.getKey(), entry.getValue());
        }
      } catch (JsonProcessingException e) {
        log.error("Error parsing response headers", e);
      }
    }

    if (apiConfig.getContentType() != null && !apiConfig.getContentType().isEmpty()) {
      responseBuilder.header("Content-Type", apiConfig.getContentType());
    }

    // Build context for template replacement
    Map<String, Object> context = RequestUtils.extractRequestContext(request, statusCode);

    // Replace template variables in responseBody (if it's a String)
    Object responseBody = apiConfig.getResponseBody();
    try {
      String responseBodyStr = "{}";
      if (responseBody instanceof String) {
        responseBodyStr = (String) responseBody;
      } else if (responseBody != null) {
        // Convert non-string response body to JSON string
        responseBodyStr = objectMapper.writeValueAsString(responseBody);
      }

      // ✅ Replace variables in JSON string
      String processedBody = applyTemplate(responseBodyStr, request, apiConfig.getStatusCode());

      // Convert processed string back to JSON
      responseBody = objectMapper.readValue(processedBody, Object.class);

      for (Map.Entry<String, Object> entry : context.entrySet()) {
        responseBodyStr = responseBodyStr.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
      }
      responseBody = responseBodyStr;

    } catch (JsonProcessingException e) {
      log.warn("Could not parse JSON response body, returning as string", e);
      responseBody = apiConfig.getResponseBody(); // Fallback to original body
    } catch (Exception e) {
      log.error("Error replacing template variables in response body", e);
      responseBody = apiConfig.getResponseBody(); // Fallback to original body
    }

    return responseBuilder.body(responseBody);
  }

  /**
   * Converts ApiConfig to ApiConfigDto.
   *
   * @param apiConfig The API configuration to convert.
   * @return The converted API configuration DTO.
   */
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

  /**
   * Applies a template to the request context, replacing variables in the
   * template
   * with values from the request.
   *
   * @param template   The template string containing variables to replace.
   * @param request    The HttpServletRequest object containing request details.
   * @param statusCode The HTTP status code to include in the context.
   * @return The processed template string with variables replaced.
   */
  private String applyTemplate(String template, HttpServletRequest request, Integer statusCode) {
    if (request == null) {
      return template; // Return original template if request is null
    }

    // Tạo context chứa thông tin từ request
    Map<String, Object> context = RequestUtils.extractRequestContext(request, statusCode);

    // Thay thế biến trong template: {{headers.User-Agent}}, {{params.name}}, ...
    Pattern pattern = Pattern.compile("\\{\\{([^}]+)}}");
    Matcher matcher = pattern.matcher(template);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String expr = matcher.group(1); // e.g., headers.cookie
      String[] parts = expr.split("\\.");
      Object value = context.get(parts[0]);

      for (int i = 1; i < parts.length && value instanceof Map; i++) {
        value = ((Map<?, ?>) value).get(parts[i]);
      }

      matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value.toString() : ""));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

}