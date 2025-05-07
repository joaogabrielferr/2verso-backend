package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;

public class InvalidRefreshTokenException extends BaseException{

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token.", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INVALID_REFRESH_TOKEN);
    }

    public InvalidRefreshTokenException(String message) {
        super(message, ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INVALID_REFRESH_TOKEN);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause, ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INVALID_REFRESH_TOKEN);
    }

}
