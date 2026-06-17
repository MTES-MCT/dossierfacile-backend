package fr.dossierfacile.api.dossierfacileapiowner.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class OwnerExceptionHandler {

    @ExceptionHandler(OwnerApiException.class)
    public ResponseEntity<OwnerApiError> handleOwnerApiException(OwnerApiException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case DPE_NOT_FOUND, VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case ADEME_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            case GENERIC -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(toOwnerApiError(exception));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OwnerApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        OwnerApiException ownerApiException = new OwnerApiException(
                OwnerApiErrorCode.VALIDATION_ERROR,
                "Validation failed",
                errors
        );
        return ResponseEntity.badRequest().body(toOwnerApiError(ownerApiException));
    }

    private static OwnerApiError toOwnerApiError(OwnerApiException exception) {
        return new OwnerApiError(
                exception.getCode().name(),
                exception.getMessage(),
                exception.getDetails(),
                exception.getErrors()
        );
    }
}
