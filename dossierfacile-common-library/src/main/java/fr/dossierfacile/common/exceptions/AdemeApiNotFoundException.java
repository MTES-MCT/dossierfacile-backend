package fr.dossierfacile.common.exceptions;

public class AdemeApiNotFoundException extends Exception {
    public AdemeApiNotFoundException(String dpeNumber) {
        super("Could not find DPE : " + dpeNumber);
    }
}
