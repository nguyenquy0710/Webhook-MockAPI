package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.dto.CreateUserRequestDto;
import vn.autobot.webhook.dto.RequestLogDto;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.RequestLogService;
import vn.autobot.webhook.service.UserService;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RequestLogService requestLogService;
    private final AppConfig appConfig;

    @GetMapping("/users")
    public String listUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("username", userDetails.getUsername());
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("userDetail", userService.getUserById(id));
        model.addAttribute("username", userDetails.getUsername());
        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUserById(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-role")
    public String toggleUserRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserRole(id);
            redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/create")
    public String showCreateUserForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("createUserRequest", new CreateUserRequestDto());
        model.addAttribute("username", userDetails.getUsername());
        return "admin/user-create";
    }

    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute CreateUserRequestDto createUserRequest, 
                           BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/user-create";
        }

        try {
            userService.createUserByAdmin(createUserRequest);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error creating user: " + e.getMessage());
            return "admin/user-create";
        }
    }

    @GetMapping("/user/@{username}/logs")
    public String viewUserRequestLogs(@PathVariable String username,
                                     @RequestParam(required = false, defaultValue = "10") int size,
                                     @RequestParam(required = false, defaultValue = "0") int page,
                                     @RequestParam(required = false) String path,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RequestLogDto> logs = requestLogService.getRequestLogs(username, path, pageable);
        long totalLogs = requestLogService.countRequestLogs(username, path);

        model.addAttribute("logs", logs);
        model.addAttribute("targetUsername", username);
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("totalLogs", totalLogs);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("domain", appConfig.getDomain());
        model.addAttribute("isAdminView", true);
        model.addAttribute("pathFilter", path != null ? path : "");

        return "request-logs";
    }

    @GetMapping("/user/@{username}/export")
    public void exportUserLogs(@PathVariable String username,
                              @AuthenticationPrincipal UserDetails userDetails,
                              HttpServletResponse response) throws IOException {
        requestLogService.exportToExcel(username, response);
    }
}

