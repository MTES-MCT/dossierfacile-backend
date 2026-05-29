package fr.dossierfacile.api.front.register;

import fr.dossierfacile.common.entity.Document;

public record DocumentSaveResult(Document document, boolean created) {
}
