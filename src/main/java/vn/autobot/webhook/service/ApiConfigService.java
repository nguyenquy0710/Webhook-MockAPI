package vn.autobot.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class ApiConfigService {
  private final ApiConfigRepository apiConfigRepository;
  private final UserService userService;
  private final ObjectMapper objectMapper;

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

  public Page<ApiConfigDto> getApiConfigsPageable(String username, int page, int size, String search) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    User user = userService.findByUsername(username);

    if (search != null && !search.isEmpty()) {
        return apiConfigRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToDto);
    }

    Page<ApiConfig> apiConfigs = apiConfigRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    return apiConfigs.map(this::convertToDto);
  }
}