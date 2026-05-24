package com.guan.rag.module.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUser {

    private String username;
    private UserRole role;
}
