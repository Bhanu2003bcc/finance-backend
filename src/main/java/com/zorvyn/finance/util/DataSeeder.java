package com.zorvyn.finance.util;

import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN user on first startup.
 * This ensures there is always at least one admin account
 * to bootstrap user and role management.
 *
 * Default credentials (change immediately in production):
 *   Email:    admin@zorvyn.io
 *   Password: Admin@1234
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL    = "admin@zorvyn.io";
    private static final String ADMIN_PASSWORD = "Admin@1234";
    private static final String ADMIN_NAME     = "System Admin";

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("DataSeeder: Admin user already exists — skipping seed.");
            return;
        }

        User admin = User.builder()
                .fullName(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);

        log.warn("============================================================");
        log.warn("  DataSeeder: Default ADMIN user created.");
        log.warn("  Email   : {}", ADMIN_EMAIL);
        log.warn("  Password: {}", ADMIN_PASSWORD);
        log.warn("  !! Change this password immediately in production !!");
        log.warn("============================================================");
    }
}
