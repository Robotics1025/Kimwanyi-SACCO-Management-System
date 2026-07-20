package org.joel.kimwanyisacco.authentication.controller;

import org.joel.kimwanyisacco.authentication.dto.LoginForm;
import org.joel.kimwanyisacco.authentication.service.AuthenticationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loginBean")
@RequestScope
public class LoginBean {

    private final AuthenticationService authenticationService;

    private LoginForm loginForm = new LoginForm();

    public LoginBean(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public LoginForm getLoginForm() {
        return loginForm;
    }

    public void setLoginForm(LoginForm loginForm) {
        this.loginForm = loginForm;
    }

    public String login() {
        return null;
    }

    public String logout() {
        return null;
    }
}
