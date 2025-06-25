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

  public DeferredResult<ResponseEntity<Object>> processWebhook(ApiConfig apiConfig) {
    DeferredResult<ResponseEntity<Object>> deferredResult = new DeferredResult<>();

    Integer delay = apiConfig.getDelayMs() != null ? apiConfig.getDelayMs() : 0;

    if (delay > 0) {
      new Thread(() -> {
        try {
          Thread.sleep(delay);
          deferredResult.setResult(buildResponse(apiConfig));
        } catch (InterruptedException e) {
          log.error("Delay interrupted", e);
          deferredResult.setErrorResult(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body("Error processing webhook"));
        }
      }).start();
    } else {
      deferredResult.setResult(buildResponse(apiConfig));
    }

    return deferredResult;
  }

  private ResponseEntity<Object> buildResponse(ApiConfig apiConfig) {
    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity
        .status(apiConfig.getStatusCode() != null ? apiConfig.getStatusCode() : 200);

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

    Object responseBody = apiConfig.getResponseBody();
    if (apiConfig.getResponseBody() != null && apiConfig.getContentType() != null
        && apiConfig.getContentType().contains("application/json")) {
      try {
        responseBody = objectMapper.readValue(apiConfig.getResponseBody(), Object.class);
      } catch (JsonProcessingException e) {
        log.warn("Could not parse JSON response body, returning as string", e);
        responseBody = apiConfig.getResponseBody();
      }
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
}