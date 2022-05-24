package fr.dossierfacile.api.dossierfacileapiowner.mail;

public class NewPasswordMailParams {
    String firstname = "";
    String tokenUrl;

    public NewPasswordMailParams(String firstname, String tokenUrl) {
        this.firstname = firstname;
        this.tokenUrl = tokenUrl;
    }
}
