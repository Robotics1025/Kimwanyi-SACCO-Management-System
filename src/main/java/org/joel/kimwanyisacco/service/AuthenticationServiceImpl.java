package org.joel.kimwanyisacco.service;

import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Override
    public LoggedInUserDto authenticate(LoginForm loginForm) {
        throw new UnsupportedOperationException("not implemented");
    }
}
