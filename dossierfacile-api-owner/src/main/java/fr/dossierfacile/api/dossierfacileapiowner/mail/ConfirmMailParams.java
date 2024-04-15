package fr.dossierfacile.api.dossierfacileapiowner.mail;

public class ConfirmMailParams {
    final String tokenUrl;

    public ConfirmMailParams(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
}
