package com.guan.rag.module.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AuthSession {

    private String token;
    private AuthUser user;
    private Instant expiresAt;
}
