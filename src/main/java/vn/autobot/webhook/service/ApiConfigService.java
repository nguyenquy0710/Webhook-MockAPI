package vn.autobot.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiConfigService {
  private final ApiConfigRepository apiConfigRepository;
  private final UserService userService;
  private final ObjectMapper objectMapper;

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

  public String backupApiConfigs(String username) {
    User user = userService.findByUsername(username);
    List<ApiConfig> apiConfigs = apiConfigRepository.findByUserOrderByCreatedAtDesc(user);
    List<ApiConfigDto> dtos = apiConfigs.stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
    try {
      return objectMapper.writeValueAsString(dtos);
    } catch (JsonProcessingException e) {
      log.error("Error serializing API configs for backup", e);
      throw new RuntimeException("Failed to backup API configurations", e);
    }
  }

  public void restoreApiConfigs(String username, String backupData) {
    User user = userService.findByUsername(username);
    try {
      List<ApiConfigDto> dtos = objectMapper.readValue(backupData, objectMapper.getTypeFactory().constructCollectionType(List.class, ApiConfigDto.class));
      for (ApiConfigDto dto : dtos) {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setUser(user);
        apiConfig.setPath(dto.getPath());
        apiConfig.setMethod(dto.getMethod());
        apiConfig.setContentType(dto.getContentType());
        apiConfig.setStatusCode(dto.getStatusCode());
        apiConfig.setResponseBody(dto.getResponseBody());
        apiConfig.setResponseHeaders(dto.getResponseHeaders());
        apiConfig.setDelayMs(dto.getDelayMs());
        apiConfigRepository.save(apiConfig);
      }
    } catch (Exception e) {
      log.error("Error deserializing API configs for restore", e);
      throw new RuntimeException("Failed to restore API configurations", e);
    }
  }
}