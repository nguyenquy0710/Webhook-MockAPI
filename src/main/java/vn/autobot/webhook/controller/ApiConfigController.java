package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.ApiConfigService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ApiConfigController {

  private final ApiMockService apiMockService;
  private final ApiConfigService apiConfigService;

  @GetMapping("/@{username}")
  public ResponseEntity<Map<String, Object>> getApiConfigs(
      @PathVariable String username,
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam int draw,
      @RequestParam int start,
      @RequestParam int length,
      @RequestParam(name = "search[value]", required = false) String searchValue,
      Pageable pageable) {

    if (!username.equals(userDetails.getUsername())) {
      return ResponseEntity.status(403).build();
    }

    int page = start / length;

    Page<ApiConfigDto> configs = apiConfigService.getApiConfigsPageable(username, page, length, searchValue);

    Map<String, Object> response = new HashMap<>();
    response.put("draw", draw);
    response.put("recordsTotal", configs.getTotalElements());
    response.put("recordsFiltered", configs.getTotalElements());
    response.put("data", configs.getContent());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/backup/@{username}")
  public ResponseEntity<String> backupApiConfigs(
      @PathVariable String username,
      @AuthenticationPrincipal UserDetails userDetails) {

    if (!username.equals(userDetails.getUsername())) {
      return ResponseEntity.status(403).build();
    }

    String backupData = apiConfigService.backupApiConfigs(username);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"api-configs-backup.json\"")
        .header("Content-Type", "application/json")
        .body(backupData);
  }

  @PostMapping("/restore/@{username}")
  public ResponseEntity<Map<String, Object>> restoreApiConfigs(
      @PathVariable String username,
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody String backupData) {

    if (!username.equals(userDetails.getUsername())) {
      return ResponseEntity.status(403).build();
    }

    try {
      apiConfigService.restoreApiConfigs(username, backupData);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "API configurations restored successfully");
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }
}