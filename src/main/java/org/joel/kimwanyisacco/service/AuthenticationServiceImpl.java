package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoggedInUserDto authenticate(LoginForm loginForm) {
        UserAccount userAccount = userAccountRepository.findByUsername(loginForm.getUsername())
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS_MESSAGE));

        if (!userAccount.isEnabled()) {
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(loginForm.getPassword(), userAccount.getPasswordHash())) {
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccount.getId());
        dto.setUsername(userAccount.getUsername());
        dto.setRoles(List.of(userAccount.getRole().name()));
        return dto;
    }
}
