package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.dto.RequestLogDto;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.RequestLogService;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class WebhookViewController {

    private final ApiMockService apiMockService;
    private final RequestLogService requestLogService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "dashboard";
    }

    @GetMapping("/api-configs")
    public String apiConfigs(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("apiConfigs", apiMockService.getApiConfigs(userDetails.getUsername()));
        model.addAttribute("newApiConfig", new ApiConfigDto());
        model.addAttribute("username", userDetails.getUsername());
        return "api-config";
    }

    @GetMapping("/api-configs/{id}")
    public String editApiConfig(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable Long id, Model model) {
        model.addAttribute("apiConfig", apiMockService.getApiConfig(userDetails.getUsername(), id));
        model.addAttribute("username", userDetails.getUsername());
        return "api-config-edit";
    }

    @PostMapping("/api-configs")
    public String saveApiConfig(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("newApiConfig") ApiConfigDto apiConfigDto,
                                BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("apiConfigs", apiMockService.getApiConfigs(userDetails.getUsername()));
            model.addAttribute("username", userDetails.getUsername());
            return "api-config";
        }

        try {
            apiMockService.saveApiConfig(userDetails.getUsername(), apiConfigDto);
            return "redirect:/api-configs?saved";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("apiConfigs", apiMockService.getApiConfigs(userDetails.getUsername()));
            model.addAttribute("username", userDetails.getUsername());
            return "api-config";
        }
    }

    @PostMapping("/api-configs/{id}")
    public String updateApiConfig(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id,
                                  @Valid @ModelAttribute("apiConfig") ApiConfigDto apiConfigDto,
                                  BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("username", userDetails.getUsername());
            return "api-config-edit";
        }

        try {
            apiConfigDto.setId(id);
            apiMockService.saveApiConfig(userDetails.getUsername(), apiConfigDto);
            return "redirect:/api-configs?updated";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("username", userDetails.getUsername());
            return "api-config-edit";
        }
    }

    @PostMapping("/api-configs/{id}/delete")
    public String deleteApiConfig(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id) {
        try {
            apiMockService.deleteApiConfig(userDetails.getUsername(), id);
            return "redirect:/api-configs?deleted";
        } catch (Exception e) {
            return "redirect:/api-configs?error=" + e.getMessage();
        }
    }

    @GetMapping("/view/@{username}")
    public String viewRequestLogs(@PathVariable String username,
                                  @PageableDefault(size = 10) Pageable pageable,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        Page<RequestLogDto> logs = requestLogService.getRequestLogs(username, pageable);
        long totalLogs = requestLogService.countRequestLogs(username);

        model.addAttribute("logs", logs);
        model.addAttribute("targetUsername", username);
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("totalLogs", totalLogs);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("totalPages", logs.getTotalPages());

        return "request-logs";
    }

    @PostMapping("/view/@{username}/delete-all")
    public String deleteAllRequestLogs(@PathVariable String username,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (username.equals(userDetails.getUsername())) {
            requestLogService.deleteAllRequestLogs(username);
            return "redirect:/view/@" + username + "?deleted";
        } else {
            return "redirect:/view/@" + username + "?error=unauthorized";
        }
    }

    @GetMapping("/view/@{username}/export")
    public void exportLogs(@PathVariable String username,
                           @AuthenticationPrincipal UserDetails userDetails,
                           HttpServletResponse response) throws IOException {
        if (username.equals(userDetails.getUsername())) {
            requestLogService.exportToExcel(username, response);
        } else {
            response.sendRedirect("/view/@" + username + "?error=unauthorized");
        }
    }
}