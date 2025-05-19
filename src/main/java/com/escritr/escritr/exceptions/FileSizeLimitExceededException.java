package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;

public class FileSizeLimitExceededException extends FileUploadException {
    public FileSizeLimitExceededException(String message, ErrorAssetEnum asset, ErrorCodeEnum code) {
        super(message,asset,code);
    }
}