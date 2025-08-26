package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.ApiConfigRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiConfigService {
  private final ApiConfigRepository apiConfigRepository;
  private final UserService userService;

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