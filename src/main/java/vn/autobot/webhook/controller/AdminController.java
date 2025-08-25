package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.autobot.webhook.dto.CreateUserRequestDto;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.service.UserService;

import javax.validation.Valid;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

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
}

