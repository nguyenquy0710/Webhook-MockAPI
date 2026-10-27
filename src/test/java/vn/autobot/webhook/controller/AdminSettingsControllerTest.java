package vn.autobot.webhook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.autobot.webhook.dto.GeneralSettingsDto;
import vn.autobot.webhook.service.DatabaseMaintenanceService;
import vn.autobot.webhook.service.RequestLogService;
import vn.autobot.webhook.service.SettingsService;
import vn.autobot.webhook.service.UserService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Admin > Settings endpoints: validation, save and maintenance run
class AdminSettingsControllerTest {

  private SettingsService settings;
  private DatabaseMaintenanceService maintenance;
  private AdminController controller;
  private LocalValidatorFactoryBean validator;
  private final UserDetails admin = new User("admin", "x", Collections.emptyList());

  @BeforeEach
  void setUp() {
    settings = mock(SettingsService.class);
    maintenance = mock(DatabaseMaintenanceService.class);
    controller = new AdminController(mock(UserService.class), mock(RequestLogService.class), settings, maintenance);
    validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
  }

  private BeanPropertyBindingResult validate(GeneralSettingsDto dto) {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(dto, "general");
    validator.validate(dto, result);
    return result;
  }

  @Test
  void settingsPageShowsStoredValuesAndMaintenanceStatus() {
    GeneralSettingsDto dto = new GeneralSettingsDto("Site", "", true, 0);
    when(settings.getGeneral()).thenReturn(dto);
    when(maintenance.getStatus()).thenReturn(mock(DatabaseMaintenanceService.Status.class));
    ConcurrentModel model = new ConcurrentModel();

    assertEquals("admin/settings", controller.settings(admin, model));

    assertEquals(dto, model.getAttribute("general"));
    assertTrue(model.containsAttribute("maintenance"));
    assertEquals("admin", model.getAttribute("username"));
  }

  @Test
  void validSettingsAreSavedAndRedirect() {
    GeneralSettingsDto dto = new GeneralSettingsDto("QuyIT", "https://mock.test", false, 30);
    RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

    String view = controller.saveGeneralSettings(dto, validate(dto), admin, new ConcurrentModel(), flash);

    assertEquals("redirect:/admin/settings", view);
    verify(settings).saveGeneral(dto);
    assertEquals("General settings saved.", flash.getFlashAttributes().get("successMessage"));
  }

  @Test
  void invalidSettingsRerenderWithoutSaving() {
    GeneralSettingsDto dto = new GeneralSettingsDto(" ", "ftp://x", true, -1);
    BeanPropertyBindingResult result = validate(dto);

    String view = controller.saveGeneralSettings(dto, result, admin, new ConcurrentModel(), new RedirectAttributesModelMap());

    assertEquals("admin/settings", view);
    assertTrue(result.hasFieldErrors("siteName"));
    assertTrue(result.hasFieldErrors("publicBaseUrl"));
    assertTrue(result.hasFieldErrors("requestLogRetentionDays"));
    verify(settings, never()).saveGeneral(any());
  }

  @Test
  void emptyPublicUrlAndZeroRetentionAreValid() {
    assertFalse(validate(new GeneralSettingsDto("Site", "", true, 0)).hasErrors());
    assertFalse(validate(new GeneralSettingsDto("Site", "https://a.test/base", true, 3650)).hasErrors());
  }

  @Test
  void runMaintenanceReportsResultOrBusy() {
    RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
    when(maintenance.run()).thenReturn(false);
    assertEquals("redirect:/admin/settings", controller.runMaintenance(flash));
    assertEquals("Maintenance is already running.", flash.getFlashAttributes().get("errorMessage"));

    DatabaseMaintenanceService.Status ok = mock(DatabaseMaintenanceService.Status.class);
    when(ok.getLastStatus()).thenReturn("success");
    when(ok.getLastSummary()).thenReturn("Checkpoint, Analyze");
    when(maintenance.run()).thenReturn(true);
    when(maintenance.getStatus()).thenReturn(ok);
    flash = new RedirectAttributesModelMap();
    controller.runMaintenance(flash);
    assertEquals("Database maintenance finished: Checkpoint, Analyze", flash.getFlashAttributes().get("successMessage"));

    DatabaseMaintenanceService.Status failed = mock(DatabaseMaintenanceService.Status.class);
    when(failed.getLastStatus()).thenReturn("error");
    when(failed.getLastSummary()).thenReturn("boom");
    when(maintenance.getStatus()).thenReturn(failed);
    flash = new RedirectAttributesModelMap();
    controller.runMaintenance(flash);
    assertEquals("Database maintenance finished: boom", flash.getFlashAttributes().get("errorMessage"));
  }
}
