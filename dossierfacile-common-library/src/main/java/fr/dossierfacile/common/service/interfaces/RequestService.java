package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.model.LightAPIInfoModel;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

public interface RequestService {

    void send(LightAPIInfoModel lightAPIInfo, String urlCallback, String partnerApiKeyCallback);
    void send(ApplicationModel applicationModel, String urlCallback, String partnerApiKeyCallback);
}
