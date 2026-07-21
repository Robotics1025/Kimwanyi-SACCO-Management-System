package org.joel.kimwanyisacco.controller;

import jakarta.faces.context.FacesContext;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.service.AuthenticationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loginBean")
@RequestScope
public class LoginBean {

    private final AuthenticationService authenticationService;
    private final UserSessionBean userSessionBean;

    private LoginForm loginForm = new LoginForm();

    public LoginBean(AuthenticationService authenticationService, UserSessionBean userSessionBean) {
        this.authenticationService = authenticationService;
        this.userSessionBean = userSessionBean;
    }

    public LoginForm getLoginForm() {
        return loginForm;
    }

    public void setLoginForm(LoginForm loginForm) {
        this.loginForm = loginForm;
    }

    public String login() {
        try {
            LoggedInUserDto loggedInUser = authenticationService.authenticate(loginForm);
            userSessionBean.setLoggedInUser(loggedInUser);
            return loggedInUser.getRoles().contains("ADMIN")
                    ? "/admin/dashboard?faces-redirect=true"
                    : "/index?faces-redirect=true";
        } catch (AuthenticationException e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }
}
