package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

public interface RequestService {
    void send(ApplicationModel applicationModel, String urlCallback, String partnerApiKeyCallback);
}
