package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.autobot.webhook.dto.ApiConfigDto;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SwaggerService {

    private final ApiMockService apiMockService;
    private final SettingsService settingsService;

    public Map<String, Object> generateSwaggerSpec(String username) {
        List<ApiConfigDto> configs = apiMockService.getApiConfigs(username);

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", username + " API");
        info.put("version", "1.0.0");
        spec.put("info", info);

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", settingsService.getDomain());
        server.put("description", "Mock Server");
        spec.put("servers", Collections.singletonList(server));

        Map<String, Object> paths = new LinkedHashMap<>();
        for (ApiConfigDto cfg : configs) {
            String fullPath = "/api/@" + username + cfg.getPath();
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(fullPath, k -> new LinkedHashMap<>());

            Map<String, Object> op = new LinkedHashMap<>();
            String description = cfg.getDescription();
            op.put("summary", description != null && !description.trim().isEmpty() ? description : "Mock API for " + fullPath);
            Map<String, Object> responses = new LinkedHashMap<>();
            int status = cfg.getStatusCode() != null ? cfg.getStatusCode() : 200;
            responses.put(String.valueOf(status), Map.of("description", "Mock response"));
            op.put("responses", responses);

            pathItem.put(cfg.getMethod().toLowerCase(), op);
        }
        spec.put("paths", paths);

        return spec;
    }

}
