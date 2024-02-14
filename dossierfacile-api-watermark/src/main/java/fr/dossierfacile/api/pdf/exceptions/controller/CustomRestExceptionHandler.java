package fr.dossierfacile.api.pdf.exceptions.controller;

import fr.dossierfacile.api.pdf.exceptions.DocumentBadRequestException;
import fr.dossierfacile.api.pdf.exceptions.DocumentNotFoundException;
import fr.dossierfacile.api.pdf.exceptions.DocumentTokenNotFoundException;
import fr.dossierfacile.api.pdf.exceptions.ExpectationFailedException;
import fr.dossierfacile.api.pdf.exceptions.InProgressException;
import fr.dossierfacile.api.pdf.exceptions.model.ApiError;
import io.sentry.Sentry;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String MESSAGE = "Sentry ID Exception: ";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(final BindException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(final MissingServletRequestPartException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(final MissingServletRequestParameterException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException ex, final WebRequest request) {

        logger.error(MESSAGE + Sentry.captureException(Objects.requireNonNull(ex)));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(final ConstraintViolationException ex, final WebRequest request) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(final NoHandlerFoundException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(final HttpRequestMethodNotSupportedException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(final HttpMediaTypeNotSupportedException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({DocumentTokenNotFoundException.class})
    public ResponseEntity<Object> handleDocumentTokenNotFoundException(final DocumentTokenNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({DocumentNotFoundException.class})
    public ResponseEntity<Object> handleDocumentNotFoundException(final DocumentNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({DocumentBadRequestException.class})
    public ResponseEntity<Object> handleDocumentBadRequestException(final DocumentBadRequestException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({ExpectationFailedException.class})
    public ResponseEntity<Object> handleExpectationFailedException(final ExpectationFailedException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.EXPECTATION_FAILED, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({InProgressException.class})
    public ResponseEntity<Object> handleInProgressException(final InProgressException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.PROCESSING, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(final Exception ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

}