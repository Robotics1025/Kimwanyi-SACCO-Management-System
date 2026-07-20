package org.joel.kimwanyisacco.service;

import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;

public interface AuthenticationService {

    LoggedInUserDto authenticate(LoginForm loginForm);
}
