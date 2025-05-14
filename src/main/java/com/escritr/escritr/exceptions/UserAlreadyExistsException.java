package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;

public class UserAlreadyExistsException extends BaseException{

    public UserAlreadyExistsException() {
        super("There is already a user with that email or username.",
                ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.USER_ALREADY_EXISTS
        );
    }

    public UserAlreadyExistsException(String message) {
        super(message,
                ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.USER_ALREADY_EXISTS
        );
    }


}


