package fr.dossierfacile.common.exceptions;

public class AdemeApiInternalServerErrorException extends Exception {
    public AdemeApiInternalServerErrorException(String message) {
        super("ADEME API Internal Server Error: " + message);
    }
}