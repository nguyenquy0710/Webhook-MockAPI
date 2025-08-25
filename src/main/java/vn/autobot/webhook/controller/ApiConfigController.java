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

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ApiConfigController {

    private final ApiMockService apiMockService;

    @GetMapping("/@{username}")
    public ResponseEntity<Page<ApiConfigDto>> getApiConfigs(@PathVariable String username,
                                                           @AuthenticationPrincipal UserDetails userDetails,
                                                           Pageable pageable) {
        if (!username.equals(userDetails.getUsername())) {
            return ResponseEntity.status(403).build();
        }

        Page<ApiConfigDto> configs = apiMockService.getApiConfigsPageable(username, pageable);
        return ResponseEntity.ok(configs);
    }
}