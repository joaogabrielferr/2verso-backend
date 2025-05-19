package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;

public class FileUploadException extends BaseException {
    public FileUploadException(String message, ErrorAssetEnum asset, ErrorCodeEnum code) {
        super(message,asset,code);
    }

    public FileUploadException(String message, Throwable cause,ErrorAssetEnum asset, ErrorCodeEnum code) {
        super(message,cause,asset,code);
    }
}