package org.joel.kimwanyisacco.config;

import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Component
public class AdminAccountSeeder {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void seedAdmin() {
        if (!userAccountRepository.existsByUsername("admin")) {
            UserAccount admin = new UserAccount();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail("admin@kimwanyisacco.com");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);

            userAccountRepository.save(admin);
            System.out.println("====== Admin account seeded successfully (admin / admin123) ======");
        }
    }
}
