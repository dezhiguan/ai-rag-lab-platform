package com.guan.rag.module.auth.service;

import com.guan.rag.module.auth.model.AuthSession;
import com.guan.rag.module.auth.model.AuthUser;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthSession create(AuthUser user, long ttlHours) {
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthSession session = new AuthSession(token, user, Instant.now().plusSeconds(ttlHours * 3600L));
        sessions.put(token, session);
        return session;
    }

    public Optional<AuthSession> findValid(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        AuthSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
