package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import org.springframework.http.HttpStatus;

public class SessionInvalidatedException extends BaseException {

    public SessionInvalidatedException() {
        super("Session invalidated. Please log in again.",
                ErrorAssetEnum.AUTHENTICATION,
                ErrorCodeEnum.SESSION_INVALIDATED
                );
    }

    public SessionInvalidatedException(String message) {
        super(message,
                ErrorAssetEnum.AUTHENTICATION,
                ErrorCodeEnum.SESSION_INVALIDATED
        );
    }

}
