package org.joel.kimwanyisacco.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.joel.kimwanyisacco.controller.UserSessionBean;
import org.joel.kimwanyisacco.model.enums.Role;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SecurityFilter implements Filter {

    private static final String[] PUBLIC_PATHS = {
            "/login.xhtml",
            "/index.xhtml",
            "/error.xhtml",
            "/access-denied.xhtml",
            "/members/register.xhtml"
    };

    private WebApplicationContext webApplicationContext;

    @Override
    public void init(FilterConfig filterConfig) {
        webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(filterConfig.getServletContext());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getServletPath();

        if (isPublicPath(path) || isFacesResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        UserSessionBean userSessionBean = webApplicationContext.getBean(UserSessionBean.class);

        if (!userSessionBean.isLoggedIn()) {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }

        if (isAdminPath(path) && !userSessionBean.getLoggedInUser().getRoles().contains(Role.ADMIN.name())) {
            response.sendRedirect(request.getContextPath() + "/access-denied.xhtml");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (publicPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFacesResource(String path) {
        return path.startsWith("/jakarta.faces.resource");
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin/");
    }
}
