package org.joel.kimwanyisacco.controller;

import jakarta.faces.context.FacesContext;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.joel.kimwanyisacco.service.AuditLogService;
import org.joel.kimwanyisacco.service.AuthenticationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loginBean")
@RequestScope
public class LoginBean {

    private final AuthenticationService authenticationService;
    private final UserSessionBean userSessionBean;
    private final AuditLogService auditLogService;
    private final UserAccountRepository userAccountRepository;

    private LoginForm loginForm = new LoginForm();

    public LoginBean(AuthenticationService authenticationService, 
                     UserSessionBean userSessionBean,
                     AuditLogService auditLogService,
                     UserAccountRepository userAccountRepository) {
        this.authenticationService = authenticationService;
        this.userSessionBean = userSessionBean;
        this.auditLogService = auditLogService;
        this.userAccountRepository = userAccountRepository;
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
                    : "/members/dashboard?faces-redirect=true";
        } catch (AuthenticationException e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public String logout() {
        if (userSessionBean.isLoggedIn()) {
            UserAccount userAccount = userAccountRepository.findById(userSessionBean.getLoggedInUser().getId()).orElse(null);
            if (userAccount != null) {
                auditLogService.record(userAccount, AuditAction.LOGOUT, "UserAccount", userAccount.getId(), null);
            }
        }
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }
}
