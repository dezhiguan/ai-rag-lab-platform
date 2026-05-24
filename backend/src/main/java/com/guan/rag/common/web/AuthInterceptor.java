package com.guan.rag.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guan.rag.common.ApiResponse;
import com.guan.rag.config.RagProperties;
import com.guan.rag.module.auth.model.AuthSession;
import com.guan.rag.module.auth.service.TokenStore;
import com.guan.rag.module.auth.support.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/system/health"
    );

    private final RagProperties ragProperties;
    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!ragProperties.getAuth().isEnabled()) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (isPublicPath(path) || isSwaggerPath(path)) {
            return true;
        }
        String token = extractToken(request);
        Optional<AuthSession> session = tokenStore.findValid(token);
        if (session.isEmpty()) {
            writeUnauthorized(response);
            return false;
        }
        AuthContext.set(session.get().getUser());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                  Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::equals);
    }

    private boolean isSwaggerPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/doc.html")
                || path.startsWith("/webjars/");
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(401, "未登录或 token 无效"));
    }
}
