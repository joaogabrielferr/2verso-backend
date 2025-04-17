package com._verso._verso.common;

public class ErrorMessage {

    private String message;
    private ErrorAssetEnum asset;

    public ErrorMessage(String message, ErrorAssetEnum asset){
        this.message = message;
        this.asset = asset;
    }

}
