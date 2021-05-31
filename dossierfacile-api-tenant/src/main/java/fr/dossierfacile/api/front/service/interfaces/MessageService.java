package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.MessageForm;
import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

public interface MessageService {
    List<MessageModel> findAll(Tenant principalAuthTenant);

    MessageModel create(Tenant principalAuthTenant, MessageForm messageForm, boolean isCustomMessage);

    void updateStatusOfDeniedDocuments(Tenant principalAuthTenant);
}
