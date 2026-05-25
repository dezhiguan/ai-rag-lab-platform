package com.guan.rag.module.auth.model;

import lombok.Getter;

@Getter
public enum UserRole {

    ADMIN("ADMIN", "管理员", "管理员模式"),
    GUEST("GUEST", "体验用户", "只读体验模式");

    private final String code;
    private final String label;
    private final String mode;

    UserRole(String code, String label, String mode) {
        this.code = code;
        this.label = label;
        this.mode = mode;
    }

    public static UserRole fromCode(String code) {
        if (code == null) {
            return GUEST;
        }
        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return GUEST;
    }
}
