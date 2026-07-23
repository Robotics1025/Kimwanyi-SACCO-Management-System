package org.joel.kimwanyisacco.config;

import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

@Component
public class AdminAccountSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;

    public AdminAccountSeeder(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.initial-admin.username}") String username,
            @Value("${app.initial-admin.password}") String password,
            @Value("${app.initial-admin.email}") String email
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @PostConstruct
    @Transactional
    public void seedAdmin() {
        if (password == null || password.isBlank()) {
            LOGGER.warn("Initial administrator was not created because INITIAL_ADMIN_PASSWORD is not set");
            return;
        }

        if (!userAccountRepository.existsByUsername(username)) {
            UserAccount admin = new UserAccount();
            admin.setUsername(username);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail(email);
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);

            userAccountRepository.save(admin);
            LOGGER.info("Initial administrator account '{}' created", username);
        }
    }
}
