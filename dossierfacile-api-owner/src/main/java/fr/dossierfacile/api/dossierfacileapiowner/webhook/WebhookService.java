package fr.dossierfacile.api.dossierfacileapiowner.webhook;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

public interface WebhookService {
    void handleWebhook(ApplicationModel applicationModel);
}
