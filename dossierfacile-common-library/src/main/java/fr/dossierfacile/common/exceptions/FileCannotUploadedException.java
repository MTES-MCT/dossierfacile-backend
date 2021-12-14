package fr.dossierfacile.common.exceptions;

public class FileCannotUploadedException extends RuntimeException {
    public FileCannotUploadedException() {
        super("The file cannot be uploaded, check the connection with the ovh service");
    }
}
