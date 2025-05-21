package com.escritr.escritr.common.enums;

public enum ErrorCodeEnum {
    TOKEN_EXPIRED("TOKEN_EXPIRED"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS"),
    NO_REFRESH_TOKEN("NO_REFRESH_TOKEN"),
    INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN"),
    INVALID_TOKEN("INVALID_TOKEN"),
    SESSION_INVALIDATED("SESSION_INVALIDATED"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    UNAUTHORIZED("UNAUTHORIZED"),
    STORAGE_ERROR("STORAGE_ERROR"),
    FILE_UPLOAD_ERROR("FILE_UPLOAD_ERROR"),
    INPUT_FORMAT_ERROR("INPUT_FORMAT_ERROR");

    private final String value;

    ErrorCodeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
