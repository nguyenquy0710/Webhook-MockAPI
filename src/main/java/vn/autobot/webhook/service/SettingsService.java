package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.dto.GeneralSettingsDto;
import vn.autobot.webhook.dto.McpSettingsDto;
import vn.autobot.webhook.model.AppSetting;
import vn.autobot.webhook.repository.AppSettingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Admin-editable settings stored in the app_settings table (cached; falls back to application.properties). */
@Service
@RequiredArgsConstructor
public class SettingsService {

    public static final String DEFAULT_SITE_NAME = "Mock Webhook";
    private static final String SITE_NAME = "general.siteName";
    private static final String PUBLIC_BASE_URL = "general.publicBaseUrl";
    private static final String ALLOW_REGISTRATION = "general.allowRegistration";
    private static final String MCP_ENABLED = "mcp.enabled";
    private static final String MCP_RATE = "mcp.rateLimitPerMinute";
    private static final String MCP_PAGE_MAX = "mcp.maxPageSize";
    private static final String MCP_ORIGINS = "mcp.extraOrigins";
    private static final String LOG_RETENTION_DAYS = "general.requestLogRetentionDays";

    private final AppSettingRepository repository;
    private final AppConfig appConfig;
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public Optional<String> get(String key) {
        return cache.computeIfAbsent(key, k -> repository.findById(k).map(AppSetting::getValue));
    }

    public void set(String key, String value) {
        repository.save(new AppSetting(key, value));
        cache.put(key, Optional.ofNullable(value));
    }

    public String getSiteName() {
        return get(SITE_NAME).filter(v -> !v.trim().isEmpty()).orElse(DEFAULT_SITE_NAME);
    }

    /** Public base URL shown in webhook URLs; no trailing slash. */
    public String getDomain() {
        String url = get(PUBLIC_BASE_URL).filter(v -> !v.trim().isEmpty()).orElse(appConfig.getDomain());
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public boolean isRegistrationEnabled() {
        return get(ALLOW_REGISTRATION).map(Boolean::parseBoolean).orElse(true);
    }

    /** Days to keep request logs; 0 (the default) keeps them forever. */
    public int getRequestLogRetentionDays() {
        try {
            return Math.max(0, get(LOG_RETENTION_DAYS).map(Integer::parseInt).orElse(0));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public GeneralSettingsDto getGeneral() {
        return new GeneralSettingsDto(getSiteName(), get(PUBLIC_BASE_URL).orElse(""), isRegistrationEnabled(), getRequestLogRetentionDays());
    }

    public void saveGeneral(GeneralSettingsDto dto) {
        set(SITE_NAME, dto.getSiteName().trim());
        set(PUBLIC_BASE_URL, dto.getPublicBaseUrl() == null ? "" : dto.getPublicBaseUrl().trim());
        set(ALLOW_REGISTRATION, String.valueOf(dto.isAllowRegistration()));
        set(LOG_RETENTION_DAYS, String.valueOf(dto.getRequestLogRetentionDays()));
    }

    public boolean isMcpEnabled() {
        return get(MCP_ENABLED).map(Boolean::parseBoolean).orElse(true);
    }

    /** MCP requests allowed per token per minute (default 60). */
    public int getMcpRateLimitPerMinute() {
        return intSetting(MCP_RATE, 60, 1, 600);
    }

    /** Largest page size MCP tools accept (default 50). */
    public int getMcpMaxPageSize() {
        return intSetting(MCP_PAGE_MAX, 50, 1, 200);
    }

    /** Browser origins, besides this site and the public base URL, allowed to call MCP. */
    public List<String> getMcpExtraOrigins() {
        List<String> origins = new ArrayList<>();
        for (String line : get(MCP_ORIGINS).orElse("").split("\\R")) {
            if (!line.trim().isEmpty()) {
                origins.add(line.trim());
            }
        }
        return origins;
    }

    public McpSettingsDto getMcp() {
        return new McpSettingsDto(isMcpEnabled(), getMcpRateLimitPerMinute(), getMcpMaxPageSize(), get(MCP_ORIGINS).orElse(""));
    }

    public void saveMcp(McpSettingsDto dto) {
        set(MCP_ENABLED, String.valueOf(dto.isEnabled()));
        set(MCP_RATE, String.valueOf(dto.getRateLimitPerMinute()));
        set(MCP_PAGE_MAX, String.valueOf(dto.getMaxPageSize()));
        StringBuilder origins = new StringBuilder();
        for (String line : (dto.getExtraOrigins() == null ? "" : dto.getExtraOrigins()).split("\\R")) {
            if (!line.trim().isEmpty()) {
                origins.append(origins.length() == 0 ? "" : "\n").append(line.trim());
            }
        }
        set(MCP_ORIGINS, origins.toString());
    }

    private int intSetting(String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, get(key).map(Integer::parseInt).orElse(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
