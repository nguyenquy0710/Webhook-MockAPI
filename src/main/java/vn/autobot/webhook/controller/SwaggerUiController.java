package vn.autobot.webhook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.autobot.webhook.service.SwaggerService;

import java.io.IOException;
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
    public String userSwaggerUi(@PathVariable String username, Model model) {
        model.addAttribute("swaggerUrl", "/swagger/@" + username + "/swagger.json");
        return "swagger-ui";
    }

    @GetMapping(value = "/swagger/@{username}/swagger.json", produces = "application/json")
    @ResponseBody
    public Map<String, Object> userSwaggerJson(@PathVariable String username) throws IOException {
        return swaggerService.readSwagger(username);
    }

    @GetMapping("/swagger/@{username}/generate")
    public String generateSwagger(@PathVariable String username) throws IOException {
        swaggerService.writeSwaggerFile(username);
        return "redirect:/swagger/@" + username;
    }
}
