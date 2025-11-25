package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.documentIA.WebhookModel;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIAService {
    void handleWebhookCallback(WebhookModel payload);
    void sendForAnalysis(MultipartFile multipartFile, File file, Document document);
}
