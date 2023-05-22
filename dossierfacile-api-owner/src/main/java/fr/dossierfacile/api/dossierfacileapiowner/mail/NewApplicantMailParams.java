package fr.dossierfacile.api.dossierfacileapiowner.mail;

import lombok.Builder;

@Builder
public class NewApplicantMailParams {
    String ownerLastname;
    String ownerFirstname;
    String tenantName;
}
