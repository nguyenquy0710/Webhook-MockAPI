package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createAdminUserIfNotExists();
    }

    private void createAdminUserIfNotExists() {
        // Check if admin user already exists
        if (userRepository.findByRole("ADMIN").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@webhook-mock.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");

            userRepository.save(admin);
            log.info("Created default admin user: username=admin, password=admin123");
            log.warn("SECURITY WARNING: Please change the default admin password after first login!");
        } else {
            log.info("Admin user already exists, skipping initialization");
        }
    }
}