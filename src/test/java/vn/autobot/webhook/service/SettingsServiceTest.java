package vn.autobot.webhook.service;

import org.junit.jupiter.api.Test;
import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.dto.GeneralSettingsDto;
import vn.autobot.webhook.model.AppSetting;
import vn.autobot.webhook.repository.AppSettingRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

  private SettingsService service(AppSettingRepository repo) {
    AppConfig config = new AppConfig();
    config.setDomain("https://default.test");
    return new SettingsService(repo, config);
  }

  @Test
  void fallsBackToDefaultsWhenNothingStored() {
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    SettingsService s = service(repo);
    assertEquals("Mock Webhook", s.getSiteName());
    assertEquals("https://default.test", s.getDomain());
    assertTrue(s.isRegistrationEnabled());
  }

  @Test
  void savedValuesWinAndTrailingSlashIsTrimmed() {
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    when(repo.save(org.mockito.ArgumentMatchers.any(AppSetting.class))).thenAnswer(i -> i.getArgument(0));
    SettingsService s = service(repo);
    s.saveGeneral(new GeneralSettingsDto(" QuyIT Mock ", "https://mock.test/", false, 14));
    assertEquals("QuyIT Mock", s.getSiteName());
    assertEquals("https://mock.test", s.getDomain());
    assertFalse(s.isRegistrationEnabled());
    assertEquals(14, s.getRequestLogRetentionDays());
  }

  @Test
  void retentionDefaultsToKeepForeverAndIgnoresGarbage() {
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    assertEquals(0, service(repo).getRequestLogRetentionDays());

    when(repo.findById("general.requestLogRetentionDays")).thenReturn(Optional.of(new AppSetting("general.requestLogRetentionDays", "abc")));
    assertEquals(0, service(repo).getRequestLogRetentionDays());
  }

  @Test
  void emptyPublicUrlUsesConfiguredDefault() {
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    when(repo.save(org.mockito.ArgumentMatchers.any(AppSetting.class))).thenAnswer(i -> i.getArgument(0));
    SettingsService s = service(repo);
    s.saveGeneral(new GeneralSettingsDto("Site", "", true, 0));
    assertEquals("https://default.test", s.getDomain());
  }

  @Test
  void mcpSettingsHaveDefaultsAndAreClamped() {
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    SettingsService s = service(repo);
    assertTrue(s.isMcpEnabled());
    assertEquals(60, s.getMcpRateLimitPerMinute());
    assertEquals(50, s.getMcpMaxPageSize());
    assertTrue(s.getMcpExtraOrigins().isEmpty());

    when(repo.findById("mcp.rateLimitPerMinute")).thenReturn(Optional.of(new AppSetting("mcp.rateLimitPerMinute", "99999")));
    when(repo.findById("mcp.maxPageSize")).thenReturn(Optional.of(new AppSetting("mcp.maxPageSize", "abc")));
    SettingsService clamped = service(repo);
    assertEquals(600, clamped.getMcpRateLimitPerMinute());
    assertEquals(50, clamped.getMcpMaxPageSize());
  }

  @Test
  void savingMcpSettingsNormalisesTheOriginList() {
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    when(repo.save(org.mockito.ArgumentMatchers.any(AppSetting.class))).thenAnswer(i -> i.getArgument(0));
    SettingsService s = service(repo);

    s.saveMcp(new vn.autobot.webhook.dto.McpSettingsDto(false, 10, 25, "  http://localhost:6274 \r\n\r\n http://a.test\n"));

    assertFalse(s.isMcpEnabled());
    assertEquals(10, s.getMcpRateLimitPerMinute());
    assertEquals(25, s.getMcpMaxPageSize());
    assertEquals(java.util.Arrays.asList("http://localhost:6274", "http://a.test"), s.getMcpExtraOrigins());
    assertEquals("http://localhost:6274\nhttp://a.test", s.getMcp().getExtraOrigins());
  }
}
