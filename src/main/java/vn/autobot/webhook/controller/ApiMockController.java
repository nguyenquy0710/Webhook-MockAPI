package vn.autobot.webhook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.RequestLogService;
import vn.autobot.webhook.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiMockController {

  private final ApiMockService apiMockService;
  private final UserService userService;
  private final RequestLogService requestLogService;
  private final ObjectMapper objectMapper;

  @RequestMapping(value = "/@{username}/**", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
      RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.HEAD })
  public DeferredResult<ResponseEntity<Object>> handleApiRequest(@PathVariable String username,
      HttpServletRequest request) {
    if (!userService.existsByUsername(username)) {
      return createErrorResult(404, "User not found");
    }

    String method = request.getMethod();
    String fullPath = request.getRequestURI();
    String basePath = "/api/@" + username;
    String apiPath = fullPath.substring(basePath.length());
    if (apiPath.isEmpty()) {
      apiPath = "/";
    }

    User user;
    try {
      user = userService.findByUsername(username);
    } catch (Exception e) {
      return createErrorResult(404, "User not found");
    }

    Optional<ApiConfig> apiConfig = apiMockService.findApiConfig(username, apiPath, method);
    if (apiConfig.isEmpty()) {
      return createErrorResult(404, "API endpoint not configured for this path and method");
    }

    // Read the request body
    String requestBody;
    try {
      requestBody = readRequestBody(request);
    } catch (IOException e) {
      log.error("Error reading request body", e);
      requestBody = "Error reading request body: " + e.getMessage();
    }

    // Convert response body to string for logging purposes
    String responseBody = apiConfig.get().getResponseBody();

    // Create deferred result for the response
    DeferredResult<ResponseEntity<Object>> result = apiMockService.processWebhook(apiConfig.get(), request);

    // Log the request after the response is processed
    String finalRequestBody = requestBody;
    result.onCompletion(() -> {
      try {
        ResponseEntity<?> response = (ResponseEntity<?>) result.getResult();
        int status = response != null ? response.getStatusCodeValue() : 500;
        requestLogService.logRequest(user, request, finalRequestBody, status, responseBody);
      } catch (Exception e) {
        log.error("Error logging request", e);
      }
    });

    return result;
  }

  private String readRequestBody(HttpServletRequest request) throws IOException {
    StringBuilder requestBody = new StringBuilder();
    try (BufferedReader reader = request.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        requestBody.append(line);
      }
    }
    return requestBody.toString();
  }

  private DeferredResult<ResponseEntity<Object>> createErrorResult(int status, String message) {
    DeferredResult<ResponseEntity<Object>> result = new DeferredResult<>();
    result.setResult(ResponseEntity.status(status).body(message));
    return result;
  }
}
