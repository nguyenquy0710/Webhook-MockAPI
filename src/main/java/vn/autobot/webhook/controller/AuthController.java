package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import vn.autobot.webhook.dto.ChangePasswordRequestDto;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.service.UserService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final UserDetailsService userDetailsService;

  @GetMapping("/login")
  public String loginPage() {
    return "login";
  }

  @GetMapping("/register")
  public String registrationPage(Model model) {
    model.addAttribute("registerRequest", new RegisterRequestDto());
    return "register";
  }

  @PostMapping("/register")
  public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequestDto registerRequest,
      BindingResult bindingResult, Model model) {
    if (bindingResult.hasErrors()) {
      return "register";
    }

    try {
      userService.registerUser(registerRequest);
      return "redirect:/login?registered";
    } catch (Exception e) {
      model.addAttribute("errorMessage", e.getMessage());
      return "register";
    }
  }

  @GetMapping("/logout")
  public String logout(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      new SecurityContextLogoutHandler().logout(request, response, auth);
    }
    return "redirect:/login?utm_origin=logout";
  }

  @GetMapping("/change-password")
  public String changePasswordPage(Model model) {
    model.addAttribute("changePasswordRequest", new ChangePasswordRequestDto());
    return "change-password";
  }

  @PostMapping("/change-password")
  public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequestDto changePasswordRequest,
      BindingResult bindingResult, Model model) {
    if (bindingResult.hasErrors()) {
      return "change-password";
    }

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      UserDetails userDetails = (UserDetails) auth.getPrincipal();
      User user = userService.findByUsername(userDetails.getUsername());
      userService.changePassword(user.getId(), changePasswordRequest.getCurrentPassword(), changePasswordRequest.getNewPassword());
      model.addAttribute("successMessage", "Password changed successfully!");
      model.addAttribute("changePasswordRequest", new ChangePasswordRequestDto());
      return "change-password";
    } catch (Exception e) {
      model.addAttribute("errorMessage", e.getMessage());
      return "change-password";
    }
  }
}
