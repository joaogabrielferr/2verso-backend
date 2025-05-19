package com.escritr.escritr.exceptions;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.common.ErrorMessage;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionsHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionsHandler.class);

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handleEndpointNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Endpoint not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "Method not allowed", "message", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> ResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(EntityConflictException.class)
    public ResponseEntity<String> handleConflict(EntityConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<?> handleInternal(InternalServerErrorException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidEntityException.class)
    public ResponseEntity<String> handleInvalidEntity(InvalidEntityException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Database constraint violated"
//                "details", ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage()
        ));
    }

    @ExceptionHandler(AuthenticationTokenException.class)
    public ResponseEntity<?> handleAuthenticationTokenExceptionHandler(AuthenticationTokenException ex){
        ErrorMessage errorMessage = new ErrorMessage(
                ex.getMessage(),
                ex.getErrorAsset(),
                ex.getErrorCode()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorMessage> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
//        log.warn("Invalid refresh token used: {}", ex.getMessage());
        ErrorMessage errorMessage = new ErrorMessage(ex.getMessage(),ex.getErrorAsset(),ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
    }

    @ExceptionHandler(SessionInvalidatedException.class)
    public ResponseEntity<ErrorMessage> handleSessionInvalidated(SessionInvalidatedException ex) {
        log.warn("Attempt to use refresh token for invalidated session: {}", ex.getMessage());
        ErrorMessage errorMessage = new ErrorMessage(ex.getMessage(),ex.getErrorAsset(),ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("Attempt to use create an user with an username of email already registered: {}", ex.getMessage());
        ErrorMessage errorMessage = new ErrorMessage(ex.getMessage(),ex.getErrorAsset(),ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAuthenticationException(AuthenticationException ex) {
        log.warn("error while authenticating: {}", ex.getMessage());

        ErrorMessage errorMessage = new ErrorMessage(
                "Invalid username or password.",
                ErrorAssetEnum.AUTHENTICATION,
                ErrorCodeEnum.INVALID_CREDENTIALS
        );
        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex,WebRequest request) {
        log.error("Unhandled exception during request to {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }








}