package fr.dossierfacile.api.front.dfc.service;

import fr.dossierfacile.common.entity.Document;

public interface DfcDocumentService {

    Document getAuthorizedDocument(String token, Long tenantId, String keycloakClientId);
}
