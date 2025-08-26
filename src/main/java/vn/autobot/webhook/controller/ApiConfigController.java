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
    private final RequestLogService requestLogService;

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

        // Tính toán trang hiện tại từ start & length
        int page = start / length;

        // Gọi service để lấy dữ liệu theo trang
        Page<ApiConfigDto> configs = apiConfigService.getApiConfigsPageable(username, page, length, searchValue);

        // Chuẩn bị dữ liệu phản hồi theo định dạng của DataTables
        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        response.put("recordsTotal", configs.getTotalElements());
        response.put("recordsFiltered", configs.getTotalElements()); // Nếu có lọc thì ghi số lượng sau lọc
        response.put("data", configs.getContent());

        return ResponseEntity.ok(response);
    }
}