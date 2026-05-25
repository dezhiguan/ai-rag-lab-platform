package com.guan.rag.module.auth.support;

import com.guan.rag.common.exception.ForbiddenException;
import com.guan.rag.module.auth.model.AuthUser;
import com.guan.rag.module.auth.model.UserRole;
import org.springframework.stereotype.Service;

@Service
public class AuthPermissionService {

    public void requireAdmin() {
        AuthUser user = AuthContext.get();
        if (user == null) {
            throw new ForbiddenException("未登录");
        }
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException();
        }
    }

    public boolean isAdmin() {
        AuthUser user = AuthContext.get();
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}
