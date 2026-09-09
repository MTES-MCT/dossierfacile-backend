package fr.dossierfacile.api.front.register;

import fr.dossierfacile.common.entity.Document;

public record DocumentSaveResult(Document document, boolean created, boolean edited) {
    public DocumentSaveResult(Document document, boolean created) {
        this(document, created, true);
    }
}
