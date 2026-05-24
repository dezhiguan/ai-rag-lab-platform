package com.guan.rag.module.auth.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private String roleLabel;
    private String mode;
}
