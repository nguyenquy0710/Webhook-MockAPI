package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public User registerUser(RegisterRequestDto registerRequest) {
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      throw new RuntimeException("Username already exists");
    }

    if (userRepository.existsByEmail(registerRequest.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
      throw new RuntimeException("Passwords do not match");
    }

    User user = new User();
    user.setUsername(registerRequest.getUsername());
    user.setEmail(registerRequest.getEmail());
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

    return userRepository.save(user);
  }

  /**
   * Finds a user by their username.
   *
   * @param username the username to search for
   * @return the User object if found
   * @throws RuntimeException if the user is not found
   */
  public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  /**
   * Checks if a user exists by their username.
   *
   * @param username the username to check
   * @return true if the user exists, false otherwise
   */
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }
}
