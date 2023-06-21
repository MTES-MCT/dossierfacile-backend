package fr.dossierfacile.process.file.barcode.twoddoc.validation;

public class TwoDDocValidationException extends RuntimeException {

    public TwoDDocValidationException(String message, Exception exception) {
        super(message, exception);
    }

    public TwoDDocValidationException(String message) {
        super(message);
    }

    public TwoDDocValidationException(Throwable cause) {
        super(cause);
    }

}
