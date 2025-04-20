package com.escritr.escritr.exceptions;

public class EntityConflictException extends RuntimeException {
    public EntityConflictException(String message, Throwable cause) { super(message, cause); }
}