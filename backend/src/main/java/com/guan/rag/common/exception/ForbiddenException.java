package com.guan.rag.common.exception;

public class ForbiddenException extends RuntimeException {

    public static final String GUEST_WRITE_DENIED = "当前为体验账号，不支持该操作";

    public ForbiddenException() {
        super(GUEST_WRITE_DENIED);
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
