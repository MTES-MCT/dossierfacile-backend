package fr.dossierfacile.common.exceptions;

public class RetryableOperationException extends RuntimeException{

    public RetryableOperationException(String message, Throwable e) {
        super(message,e);
    }

    public RetryableOperationException(String message) {
        super(message);
    }
}
