package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;

public class BaseException extends RuntimeException{
    private final ErrorAssetEnum errorAsset;
    private final ErrorCodeEnum errorCode;

    public BaseException(String message, ErrorAssetEnum errorAsset, ErrorCodeEnum errorCode) {
        super(message);
        this.errorAsset = errorAsset;
        this.errorCode = errorCode;
    }

    public BaseException(String message,Throwable cause, ErrorAssetEnum errorAsset, ErrorCodeEnum errorCode) {
        super(message, cause);
        this.errorAsset = errorAsset;
        this.errorCode = errorCode;
    }

    public ErrorAssetEnum getErrorAsset(){
        return this.errorAsset;
    }

    public ErrorCodeEnum getErrorCode(){
        return this.errorCode;
    }
}
