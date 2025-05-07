package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;

public class AuthenticationTokenException extends BaseException{
    public AuthenticationTokenException(String message, ErrorAssetEnum asset, ErrorCodeEnum code){super(message,asset,code);}


}
