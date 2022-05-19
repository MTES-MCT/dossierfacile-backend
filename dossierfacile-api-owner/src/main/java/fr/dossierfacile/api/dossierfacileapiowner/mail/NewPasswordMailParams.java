package fr.dossierfacile.api.dossierfacileapiowner.mail;

public class NewPasswordMailParams {
    String firstname = "";
    String token;

    public NewPasswordMailParams(String firstname, String token) {
        this.firstname = firstname;
        this.token = token;
    }
}
