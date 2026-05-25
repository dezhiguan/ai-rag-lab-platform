package com.guan.rag.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guan.rag.common.ApiResponse;
import com.guan.rag.common.exception.ForbiddenException;
import com.guan.rag.module.auth.model.AuthUser;
import com.guan.rag.module.auth.model.UserRole;
import com.guan.rag.module.auth.support.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * guest 体验账号禁止写操作与危险管理接口。
 */
@Component
@RequiredArgsConstructor
public class GuestWriteInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final Pattern[] GUEST_FORBIDDEN_PATTERNS = {
            Pattern.compile("^POST /api/kb$"),
            Pattern.compile("^DELETE /api/kb/\\d+$"),
            Pattern.compile("^POST /api/kb/\\d+/documents/upload$"),
            Pattern.compile("^POST /api/sample/init$"),
            Pattern.compile("^POST /api/kb/\\d+/embedding/rebuild$"),
            Pattern.compile("^POST /api/search/index/rebuild$"),
    };

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        AuthUser user = AuthContext.get();
        if (user == null || user.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (!WRITE_METHODS.contains(request.getMethod())) {
            return true;
        }
        String signature = request.getMethod() + " " + request.getRequestURI();
        for (Pattern pattern : GUEST_FORBIDDEN_PATTERNS) {
            if (pattern.matcher(signature).matches()) {
                writeForbidden(response);
                return false;
            }
        }
        return true;
    }

    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(403, ForbiddenException.GUEST_WRITE_DENIED));
    }
}
