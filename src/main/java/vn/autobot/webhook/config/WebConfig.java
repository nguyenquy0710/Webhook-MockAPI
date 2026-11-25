package vn.autobot.webhook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Public endpoints only, without credentials; every other path uses the session cookie and gets no CORS headers.
        // "/api/@*/**" does not match /api/logs/** and /api/configs/**. MCP checks Origin itself.
        for (String path : new String[] {"/api/@*/**", "/mcp/**"}) {
            registry.addMapping(path)
                    .allowedOrigins("*")
                    .allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .maxAge(3600);
        }
    }
}