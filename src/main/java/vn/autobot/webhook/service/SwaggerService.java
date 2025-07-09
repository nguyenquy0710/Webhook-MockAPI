package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.dto.ApiConfigDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SwaggerService {

    private final ApiMockService apiMockService;
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;

    public Path getSwaggerPath(String username) {
        return Paths.get("data", "swagger", username, "swagger.json");
    }

    public Map<String, Object> generateSwaggerSpec(String username) {
        List<ApiConfigDto> configs = apiMockService.getApiConfigs(username);

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", username + " API");
        info.put("version", "1.0.0");
        spec.put("info", info);

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", appConfig.getDomain());
        server.put("description", "Mock Server");
        spec.put("servers", Collections.singletonList(server));

        Map<String, Object> paths = new LinkedHashMap<>();
        for (ApiConfigDto cfg : configs) {
            String fullPath = "/api/@" + username + cfg.getPath();
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(fullPath, k -> new LinkedHashMap<>());

            Map<String, Object> op = new LinkedHashMap<>();
            op.put("summary", "Mock API for " + fullPath);
            Map<String, Object> responses = new LinkedHashMap<>();
            int status = cfg.getStatusCode() != null ? cfg.getStatusCode() : 200;
            responses.put(String.valueOf(status), Map.of("description", "Mock response"));
            op.put("responses", responses);

            pathItem.put(cfg.getMethod().toLowerCase(), op);
        }
        spec.put("paths", paths);

        return spec;
    }

    public void writeSwaggerFile(String username) throws IOException {
        Map<String, Object> spec = generateSwaggerSpec(username);
        Path path = getSwaggerPath(username);
        Files.createDirectories(path.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), spec);
    }

    public Map<String, Object> readSwagger(String username) throws IOException {
        Path path = getSwaggerPath(username);
        if (!Files.exists(path)) {
            writeSwaggerFile(username);
        }
        return objectMapper.readValue(path.toFile(), Map.class);
    }
}
