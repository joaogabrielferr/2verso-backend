package com.escritr.escritr.common;

import org.flywaydb.core.api.ErrorCode;

public class ErrorMessage {

    private final String message;
    private final ErrorAssetEnum asset;
    private final ErrorCodeEnum errorCode;

    public ErrorMessage(String message, ErrorAssetEnum asset, ErrorCodeEnum errorCode){
        this.message = message;
        this.asset = asset;
        this.errorCode = errorCode;
    }


    public String getMessage() { return message; }
    public ErrorAssetEnum getAsset() { return asset; }
    public ErrorCodeEnum getErrorCode(){return errorCode;}
}
