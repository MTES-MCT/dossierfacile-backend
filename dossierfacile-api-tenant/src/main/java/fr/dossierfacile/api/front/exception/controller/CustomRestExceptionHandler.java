package fr.dossierfacile.api.front.exception.controller;

import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.exception.model.ApiError;
import io.sentry.Sentry;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
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

import javax.annotation.CheckForNull;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String MESSAGE = "Sentry ID Exception: ";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@CheckForNull final MethodArgumentNotValidException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final List<String> errors = new ArrayList<>();
        for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(@CheckForNull final BindException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final List<String> errors = new ArrayList<>();
        for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(@CheckForNull final TypeMismatchException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final String error = ex.getValue() + " value for " + ex.getPropertyName() + " should be of type " + ex.getRequiredType();

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), error);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(@CheckForNull final MissingServletRequestPartException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final String error = ex.getRequestPartName() + " part is missing";
        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), error);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(@CheckForNull final MissingServletRequestParameterException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final String error = ex.getParameterName() + " parameter is missing";
        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), error);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(@CheckForNull final MethodArgumentTypeMismatchException ex, final WebRequest request) {

        logger.error(MESSAGE + Sentry.captureException(Objects.requireNonNull(ex)));
        logger.error(ex.getMessage(), ex);

        final String error = ex.getName() + " should be of type " + Objects.requireNonNull(ex.getRequiredType()).getName();

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), error);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(final ConstraintViolationException ex, final WebRequest request) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final List<String> errors = new ArrayList<>();
        for (final ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getRootBeanClass().getName() + " " + violation.getPropertyPath() + ": " + violation.getMessage());
        }

        final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(@CheckForNull final NoHandlerFoundException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final String error = "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL();

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage(), error);
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(@CheckForNull final HttpRequestMethodNotSupportedException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final StringBuilder builder = new StringBuilder();
        builder.append(ex.getMethod());
        builder.append(" method is not supported for this request. Supported methods are ");
        Objects.requireNonNull(ex.getSupportedHttpMethods()).forEach(t -> builder.append(t).append(" "));

        final ApiError apiError = new ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.getLocalizedMessage(), builder.toString());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(@CheckForNull final HttpMediaTypeNotSupportedException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        assert ex != null;
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final StringBuilder builder = new StringBuilder();
        builder.append(ex.getContentType());
        builder.append(" media type is not supported. Supported media types are ");
        ex.getSupportedMediaTypes().forEach(t -> builder.append(t).append(" "));

        final ApiError apiError = new ApiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getLocalizedMessage(), builder.substring(0, builder.length() - 2));
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<Object> handleBadCredential(final BadCredentialsException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({GuarantorNotFoundException.class})
    public ResponseEntity<Object> handleGuarantorNotFoundException(final GuarantorNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        ex.printStackTrace();
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<Object> handleUserNotFoundException(final UserNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({PasswordRecoveryTokenNotFoundException.class})
    public ResponseEntity<Object> handlePasswordRecoveryTokenNotFoundException(final PasswordRecoveryTokenNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<Object> handleFileNotFoundException(final FileNotFoundException ex) {
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

    @ExceptionHandler({ConfirmationTokenNotFoundException.class})
    public ResponseEntity<Object> handleConfirmationTokenNotFoundException(final ConfirmationTokenNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({ApartmentSharingNotFoundException.class})
    public ResponseEntity<Object> handleApartmentSharingNotFoundException(final ApartmentSharingNotFoundException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({ApartmentSharingUnexpectedException.class})
    public ResponseEntity<Object> handleApartmentSharingUnexpectedException(final ApartmentSharingUnexpectedException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.EXPECTATION_FAILED, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Object> handleAccessDeniedException(final AccessDeniedException ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, ex.getLocalizedMessage());
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(final Exception ex) {
        logger.error(MESSAGE + Sentry.captureException(ex));
        logger.error(ex.getMessage(), ex);

        final ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage(), "error occurred");
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

}