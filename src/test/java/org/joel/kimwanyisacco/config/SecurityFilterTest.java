package org.joel.kimwanyisacco.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.joel.kimwanyisacco.controller.UserSessionBean;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

class SecurityFilterTest {

    private final ServletContext servletContext = mock(ServletContext.class);
    private final FilterConfig filterConfig = mock(FilterConfig.class);
    private final WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
    private final UserSessionBean userSessionBean = mock(UserSessionBean.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    private SecurityFilter filter;

    @BeforeEach
    void setUp() {
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(webApplicationContext.getBean(UserSessionBean.class)).thenReturn(userSessionBean);
        when(request.getContextPath()).thenReturn("/kimwanyi-sacco");

        try (MockedStatic<WebApplicationContextUtils> mockedStatic = mockStatic(WebApplicationContextUtils.class)) {
            mockedStatic.when(() -> WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext))
                    .thenReturn(webApplicationContext);
            filter = new SecurityFilter();
            assertDoesNotThrow(() -> filter.init(filterConfig));
        }
    }

    @Test
    void allowsWhitelistedPathThroughWithoutCheckingSession() throws Exception {
        when(request.getServletPath()).thenReturn("/login.xhtml");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void redirectsToLoginWhenNotAuthenticated() throws Exception {
        when(request.getServletPath()).thenReturn("/members/list.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/kimwanyi-sacco/login.xhtml");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void redirectsToAccessDeniedWhenMemberRequestsAdminPath() throws Exception {
        when(request.getServletPath()).thenReturn("/admin/dashboard.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(true);
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("MEMBER"));
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/kimwanyi-sacco/access-denied.xhtml");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsAdminThroughToAdminPath() throws Exception {
        when(request.getServletPath()).thenReturn("/admin/dashboard.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(true);
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("ADMIN"));
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsLoggedInMemberThroughToMemberPath() throws Exception {
        when(request.getServletPath()).thenReturn("/members/list.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
