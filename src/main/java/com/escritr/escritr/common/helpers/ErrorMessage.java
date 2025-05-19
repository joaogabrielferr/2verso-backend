package com.escritr.escritr.common.helpers;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;

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
