package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginBeanTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserSessionBean userSessionBean;

    private LoginForm formFor(String username, String password) {
        LoginForm form = new LoginForm();
        form.setUsername(username);
        form.setPassword(password);
        return form;
    }

    @Test
    void loginRedirectsToAdminDashboardForAdminUser() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);
        loginBean.setLoginForm(formFor("admin1", "secret"));
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("ADMIN"));
        when(authenticationService.authenticate(loginBean.getLoginForm())).thenReturn(dto);

        String outcome = loginBean.login();

        assertEquals("/admin/dashboard?faces-redirect=true", outcome);
        verify(userSessionBean).setLoggedInUser(dto);
    }

    @Test
    void loginRedirectsToIndexForMemberUser() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);
        loginBean.setLoginForm(formFor("member1", "secret"));
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("MEMBER"));
        when(authenticationService.authenticate(loginBean.getLoginForm())).thenReturn(dto);

        String outcome = loginBean.login();

        assertEquals("/index?faces-redirect=true", outcome);
        verify(userSessionBean).setLoggedInUser(dto);
    }

    @Test
    void loginReturnsNullAndAddsErrorMessageOnBadCredentials() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);
        loginBean.setLoginForm(formFor("member1", "wrong"));
        when(authenticationService.authenticate(loginBean.getLoginForm()))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        FacesContext facesContext = mock(FacesContext.class);

        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = loginBean.login();

            assertNull(outcome);
            verify(facesContext).addMessage(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void logoutInvalidatesSessionAndReturnsLoginOutcome() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);

        FacesContext facesContext = mock(FacesContext.class);
        ExternalContext externalContext = mock(ExternalContext.class);
        when(facesContext.getExternalContext()).thenReturn(externalContext);

        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = loginBean.logout();

            assertEquals("/login?faces-redirect=true", outcome);
            verify(externalContext).invalidateSession();
        }
    }
}
