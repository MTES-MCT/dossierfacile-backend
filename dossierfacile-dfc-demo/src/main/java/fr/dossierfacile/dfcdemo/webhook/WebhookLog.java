package fr.dossierfacile.dfcdemo.webhook;

import java.time.LocalDateTime;

/** One webhook received from DossierFacile, stored as the raw body. */
public record WebhookLog(long id, LocalDateTime createdDate, String body) {
}
