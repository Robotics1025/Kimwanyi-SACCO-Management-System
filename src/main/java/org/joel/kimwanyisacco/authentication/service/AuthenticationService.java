package org.joel.kimwanyisacco.authentication.service;

import org.joel.kimwanyisacco.authentication.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.authentication.dto.LoginForm;

public interface AuthenticationService {

    LoggedInUserDto authenticate(LoginForm loginForm);
}
