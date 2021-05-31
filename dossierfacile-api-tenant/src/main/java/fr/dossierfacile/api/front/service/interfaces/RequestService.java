package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.LightAPIInfoModel;
import fr.dossierfacile.api.front.model.apartment_sharing.ApplicationModel;

public interface RequestService {

    void send(LightAPIInfoModel lightAPIInfo, String urlCallback, String partnerApiKeyCallback);
    void send(ApplicationModel applicationModel, String urlCallback, String partnerApiKeyCallback);
}
