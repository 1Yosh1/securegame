package com.unime.securegame.config;

import com.unime.securegame.service.AdminApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private final AdminApiKeyService adminApiKeyService;

    public AdminApiKeyFilter(AdminApiKeyService adminApiKeyService) {
        this.adminApiKeyService = adminApiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAdminEndpoint(request) && adminApiKeyService.isValid(request.getHeader("X-Admin-Key"))) {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    "api-admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            // API-key authentication is request-scoped; never persist it in the user's HTTP session.
            SecurityContextHolder.setContext(context);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAdminEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/scenarios") || path.matches("/api/scenarios/\\d+")
                || path.equals("/api/risk/train") || path.startsWith("/api/risk/coach/")
                || path.equals("/api/players") || path.startsWith("/api/players/")
                || path.equals("/api/classrooms") || path.startsWith("/api/classrooms/");
    }
}
