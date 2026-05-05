package com.urbanvogue.apigateway.filter;

import com.urbanvogue.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class AuthenticationFilter extends
        AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            String path = exchange.getRequest()
                    .getURI().getPath();

            // ── PUBLIC PATHS — No token needed ──
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // ── CHECK: Auth header present? ──
            if (!exchange.getRequest().getHeaders()
                    .containsKey(HttpHeaders.AUTHORIZATION)) {
                exchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // ── EXTRACT token ──
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .get(HttpHeaders.AUTHORIZATION)
                    .get(0);

            if (authHeader != null &&
                    authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            // ── VALIDATE and inject username ──
            try {
                jwtUtil.validateToken(authHeader);
                String username =
                        jwtUtil.extractUsername(authHeader);

                // ✅ CORRECT WAY — must use exchange.mutate()
                ServerWebExchange mutatedExchange = exchange
                        .mutate()
                        .request(exchange.getRequest()
                                .mutate()
                                .header("X-Logged-In-User",
                                        username)
                                .build())
                        .build();

                return chain.filter(mutatedExchange);

            } catch (Exception e) {
                System.out.println(
                        "❌ Invalid token: " + e.getMessage()
                );
                exchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        });
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/products") ||
                path.startsWith("/api/users/exists/") ||
                path.contains("/reduce-stock") ||
                path.contains("/restore-stock") ||
                path.startsWith("/api/notifications/") ||
                path.startsWith("/api/admin/");
    }

    public static class Config { }
}

//