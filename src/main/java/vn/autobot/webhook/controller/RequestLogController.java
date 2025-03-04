package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import vn.autobot.webhook.dto.RequestLogDto;
import vn.autobot.webhook.service.RequestLogService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class RequestLogController {

    private final RequestLogService requestLogService;

    @GetMapping("/@{username}")
    public ResponseEntity<Page<RequestLogDto>> getLogs(@PathVariable String username,
                                                       @AuthenticationPrincipal UserDetails userDetails,
                                                       Pageable pageable) {
        if (!username.equals(userDetails.getUsername())) {
            return ResponseEntity.status(403).build();
        }

        Page<RequestLogDto> logs = requestLogService.getRequestLogs(username, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/@{username}/count")
    public ResponseEntity<Map<String, Object>> getLogsCount(@PathVariable String username,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (!username.equals(userDetails.getUsername())) {
            return ResponseEntity.status(403).build();
        }

        long count = requestLogService.countRequestLogs(username);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/@{username}")
    public ResponseEntity<Void> deleteAllLogs(@PathVariable String username,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        if (!username.equals(userDetails.getUsername())) {
            return ResponseEntity.status(403).build();
        }

        requestLogService.deleteAllRequestLogs(username);
        return ResponseEntity.ok().build();
    }
}