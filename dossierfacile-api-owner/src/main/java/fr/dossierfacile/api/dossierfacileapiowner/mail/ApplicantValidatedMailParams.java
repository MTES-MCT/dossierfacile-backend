package fr.dossierfacile.api.dossierfacileapiowner.mail;

import lombok.Builder;

@Builder
public class ApplicantValidatedMailParams {
    String ownerLastname;
    String ownerFirstname;
    String tenantName;
}
