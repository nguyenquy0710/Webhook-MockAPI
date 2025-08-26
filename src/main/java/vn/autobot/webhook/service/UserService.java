package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.autobot.webhook.dto.CreateUserRequestDto;
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
        user.setRole("USER");

        return userRepository.save(user);
    }

    @Transactional
    public User createUserByAdmin(CreateUserRequestDto createUserRequest) {
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (!createUserRequest.getPassword().equals(createUserRequest.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        user.setRole(createUserRequest.getRole());
        user.setEnabled(createUserRequest.getEnabled());

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void deleteUserById(Long id) {
        User user = getUserById(id);
        // Prevent deletion of the last admin user
        if ("ADMIN".equals(user.getRole()) && userRepository.findByRole("ADMIN").size() <= 1) {
            throw new RuntimeException("Cannot delete the last admin user");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User toggleUserRole(Long id) {
        User user = getUserById(id);
        
        if ("USER".equals(user.getRole())) {
            user.setRole("ADMIN");
        } else if ("ADMIN".equals(user.getRole())) {
            // Prevent demoting the last admin user
            if (userRepository.findByRole("ADMIN").size() <= 1) {
                throw new RuntimeException("Cannot demote the last admin user");
            }
            user.setRole("USER");
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User toggleUserStatus(Long id) {
        User user = getUserById(id);
        
        // Prevent disabling the last admin user
        if ("ADMIN".equals(user.getRole()) && user.getEnabled() && userRepository.findByRole("ADMIN").size() <= 1) {
            throw new RuntimeException("Cannot disable the last admin user");
        }
        
        user.setEnabled(!user.getEnabled());
        return userRepository.save(user);
    }
}
