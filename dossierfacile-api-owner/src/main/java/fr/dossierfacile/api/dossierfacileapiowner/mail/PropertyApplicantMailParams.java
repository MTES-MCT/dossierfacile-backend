package fr.dossierfacile.api.dossierfacileapiowner.mail;

import lombok.Builder;

@Builder
public class PropertyApplicantMailParams {
    private String ownerLastname;
    private String ownerFirstname;
    private String tenantName;
    private String propertyName;
    private String propertyUrl;
}