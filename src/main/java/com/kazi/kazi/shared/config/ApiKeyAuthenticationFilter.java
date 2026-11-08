package com.kazi.kazi.shared.config;

import com.kazi.kazi.apikey.ApiKeyAuthentication;
import com.kazi.kazi.apikey.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final String adminApiKey;
    private final boolean authenticationRequired;

    ApiKeyAuthenticationFilter(
        ApiKeyService apiKeyService,
        @Value("${kazi.security.api-key:}") String adminApiKey,
        @Value("${kazi.security.required:false}") boolean authenticationRequired
    ) {
        if (authenticationRequired && adminApiKey.isBlank()) {
            throw new IllegalStateException("kazi.security.api-key must not be blank when authentication is required");
        }
        this.apiKeyService = apiKeyService;
        this.adminApiKey = adminApiKey;
        this.authenticationRequired = authenticationRequired;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/mcp")
            && !path.startsWith("/actuator/metrics")
            && !path.startsWith("/api/admin/api-keys");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String candidate = request.getHeader("X-API-Key");
        if (!authenticationRequired && adminApiKey.isBlank()) {
            authenticateLocalClient();
            filterChain.doFilter(request, response);
            return;
        }
        if (matchesAdmin(candidate)) {
            authenticateAdmin();
            filterChain.doFilter(request, response);
            return;
        }
        if (!request.getRequestURI().startsWith("/api/admin/api-keys")) {
            var authentication = apiKeyService.authenticate(candidate);
            if (authentication.isPresent()) {
                authenticateUser(authentication.get());
                filterChain.doFilter(request, response);
                return;
            }
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean matchesAdmin(String candidate) {
        return !adminApiKey.isBlank()
            && candidate != null
            && MessageDigest.isEqual(
                adminApiKey.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8)
            );
    }

    private void authenticateAdmin() {
        authenticate("admin", List.of("ROLE_ADMIN", "ROLE_MCP_CLIENT", "SCOPE_jobs:read", "SCOPE_metrics:read"));
    }

    private void authenticateLocalClient() {
        authenticate("local-client", List.of("ROLE_ADMIN", "ROLE_MCP_CLIENT", "SCOPE_jobs:read", "SCOPE_metrics:read"));
    }

    private void authenticateUser(ApiKeyAuthentication authentication) {
        var authorities = new ArrayList<>(List.of("ROLE_MCP_CLIENT"));
        authentication.scopes().forEach(scope -> authorities.add("SCOPE_" + scope));
        authenticate(authentication.owner(), authorities);
    }

    private void authenticate(String owner, List<String> authorities) {
        var authentication = new UsernamePasswordAuthenticationToken(
            owner,
            null,
            authorities.stream().map(SimpleGrantedAuthority::new).toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
