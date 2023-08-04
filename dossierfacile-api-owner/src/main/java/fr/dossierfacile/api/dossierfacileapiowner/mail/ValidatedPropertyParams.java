package fr.dossierfacile.api.dossierfacileapiowner.mail;

import lombok.Builder;

@Builder
public class ValidatedPropertyParams {
    private String firstName;
    private String lastName;
    private String propertyName;
    private String propertyUrl;
}
