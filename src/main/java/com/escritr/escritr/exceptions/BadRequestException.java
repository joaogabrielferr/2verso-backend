package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;

public class BadRequestException extends BaseException {
    public BadRequestException(String message, ErrorAssetEnum asset, ErrorCodeEnum error) { super(message,asset,error); }
}