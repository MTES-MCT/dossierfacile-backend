package fr.dossierfacile.common.exceptions;

public class RetryableOperationException extends Exception{

    public RetryableOperationException(String message, Throwable e) {
        super(message,e);
    }

    public RetryableOperationException(String message) {
        super(message);
    }
}
