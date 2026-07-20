package org.joel.kimwanyisacco.authentication.service;

import org.joel.kimwanyisacco.authentication.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.authentication.dto.LoginForm;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Override
    public LoggedInUserDto authenticate(LoginForm loginForm) {
        throw new UnsupportedOperationException("not implemented");
    }
}
