package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;

public class WrongParameterException extends BaseException {

    public WrongParameterException(String message, ErrorAssetEnum asset, ErrorCodeEnum code) {
        super(message, asset, code);
    }

}
