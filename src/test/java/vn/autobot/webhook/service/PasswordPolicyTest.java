package vn.autobot.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.autobot.webhook.dto.ChangePasswordRequestDto;
import vn.autobot.webhook.dto.CreateUserRequestDto;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.UserRepository;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Rules for a new password: 8 characters or more, not a common one, not the username
class PasswordPolicyTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private UserRepository repository;
  private UserService service;

  @BeforeEach
  void setUp() {
    repository = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(encoder.matches(any(), any())).thenReturn(true);
    User user = new User();
    user.setUsername("someuser1");
    when(repository.findById(1L)).thenReturn(Optional.of(user));
    service = new UserService(repository, encoder);
  }

  @Test
  void sevenCharactersAreTooShortEverywhere() {
    ChangePasswordRequestDto change = new ChangePasswordRequestDto();
    CreateUserRequestDto create = new CreateUserRequestDto();
    for (String password : new String[] {"abcdef1", "abcdef12"}) {
      int expected = password.length() < 8 ? 1 : 0;
      change.setNewPassword(password);
      create.setPassword(password);
      assertEquals(expected, validator.validateProperty(new RegisterRequestDto("u", password, password, "u@e.com"), "password").size());
      assertEquals(expected, validator.validateProperty(change, "newPassword").size());
      assertEquals(expected, validator.validateProperty(create, "password").size());
    }
  }

  @Test
  void commonPasswordsAndTheUsernameAreRefused() {
    for (String password : new String[] {"password", "PassWord123", "12345678", "admin123", "SomeUser1"}) {
      CreateUserRequestDto create = new CreateUserRequestDto();
      create.setUsername("someuser1");
      create.setPassword(password);
      create.setConfirmPassword(password);

      assertThrows(RuntimeException.class, () -> service.registerUser(new RegisterRequestDto("someuser1", password, password, "u@e.com")));
      assertThrows(RuntimeException.class, () -> service.createUserByAdmin(create));
      assertThrows(RuntimeException.class, () -> service.changePassword(1L, "old", password));
    }
    verify(repository, never()).save(any());
  }

  @Test
  void otherPasswordsAreAccepted() {
    service.registerUser(new RegisterRequestDto("someuser1", "tram-cam-92", "tram-cam-92", "u@e.com"));
    service.changePassword(1L, "old", "tram-cam-92");

    verify(repository, times(2)).save(any());
  }
}
