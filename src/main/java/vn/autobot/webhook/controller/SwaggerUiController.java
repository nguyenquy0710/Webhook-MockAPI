package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import vn.autobot.webhook.service.SwaggerService;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SwaggerUiController {

    private final SwaggerService swaggerService;

    @GetMapping("/swagger-ui")
    public String swaggerUi(Model model) {
        model.addAttribute("swaggerUrl", "/swagger.json");
        return "swagger-ui";
    }

    @GetMapping("/swagger/@{username}")
    public String userSwaggerUi(@PathVariable String username, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        requireOwner(username, userDetails);
        model.addAttribute("swaggerUrl", "/swagger/@" + username + "/swagger.json");
        return "swagger-ui";
    }

    @GetMapping(value = "/swagger/@{username}/swagger.json", produces = "application/json")
    @ResponseBody
    public Map<String, Object> userSwaggerJson(@PathVariable String username, @AuthenticationPrincipal UserDetails userDetails) {
        requireOwner(username, userDetails);
        return swaggerService.generateSwaggerSpec(username);
    }

    @GetMapping("/swagger/@{username}/generate")
    public String generateSwagger(@PathVariable String username) {
        return "redirect:/swagger/@" + username; // the spec is always current; kept for old links
    }

    private static void requireOwner(String username, UserDetails userDetails) {
        if (!username.equals(userDetails.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
