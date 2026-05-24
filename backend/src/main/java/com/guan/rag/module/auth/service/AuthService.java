package com.guan.rag.module.auth.service;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.common.exception.UnauthorizedException;
import com.guan.rag.config.RagProperties;
import com.guan.rag.module.auth.model.AuthSession;
import com.guan.rag.module.auth.model.AuthUser;
import com.guan.rag.module.auth.model.UserRole;
import com.guan.rag.module.auth.request.LoginRequest;
import com.guan.rag.module.auth.response.LoginResponse;
import com.guan.rag.module.auth.response.MeResponse;
import com.guan.rag.module.auth.support.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RagProperties ragProperties;
    private final TokenStore tokenStore;

    public LoginResponse login(LoginRequest request) {
        AuthUser user = authenticate(request.getUsername(), request.getPassword());
        AuthSession session = tokenStore.create(user, ragProperties.getAuth().getTokenTtlHours());
        return toLoginResponse(session);
    }

    public MeResponse me() {
        AuthUser user = requireCurrentUser();
        return toMeResponse(user);
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    public AuthUser requireCurrentUser() {
        AuthUser user = AuthContext.get();
        if (user == null) {
            throw new UnauthorizedException("未登录或 token 无效");
        }
        return user;
    }

    public AuthUser authenticate(String username, String password) {
        Map<String, RagProperties.AuthAccount> accounts = ragProperties.getAuth().getAccounts();
        RagProperties.AuthAccount account = accounts.get(username);
        if (account == null || account.getPassword() == null || !account.getPassword().equals(password)) {
            throw new BusinessException(401, "账号或密码错误");
        }
        UserRole role = UserRole.fromCode(account.getRole());
        return new AuthUser(username, role);
    }

    private LoginResponse toLoginResponse(AuthSession session) {
        AuthUser user = session.getUser();
        UserRole role = user.getRole();
        return LoginResponse.builder()
                .token(session.getToken())
                .username(user.getUsername())
                .role(role.getCode())
                .roleLabel(role.getLabel())
                .mode(role.getMode())
                .build();
    }

    private MeResponse toMeResponse(AuthUser user) {
        UserRole role = user.getRole();
        return MeResponse.builder()
                .username(user.getUsername())
                .role(role.getCode())
                .roleLabel(role.getLabel())
                .mode(role.getMode())
                .build();
    }
}
