package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;

public class AuthenticationTokenException extends BaseException{
    public AuthenticationTokenException(String message, ErrorAssetEnum asset, ErrorCodeEnum code){super(message,asset,code);}


}
