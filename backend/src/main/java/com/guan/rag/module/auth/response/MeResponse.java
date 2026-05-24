package com.guan.rag.module.auth.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeResponse {

    private String username;
    private String role;
    private String roleLabel;
    private String mode;
}
