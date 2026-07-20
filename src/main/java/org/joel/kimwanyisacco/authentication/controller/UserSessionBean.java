package org.joel.kimwanyisacco.authentication.controller;

import java.io.Serializable;
import org.joel.kimwanyisacco.authentication.dto.LoggedInUserDto;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("userSessionBean")
@SessionScope
public class UserSessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private LoggedInUserDto loggedInUser;

    public LoggedInUserDto getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(LoggedInUserDto loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }
}
