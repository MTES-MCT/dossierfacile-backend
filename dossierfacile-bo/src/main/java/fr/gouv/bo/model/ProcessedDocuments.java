package fr.gouv.bo.model;

import fr.gouv.bo.dto.CustomMessage;

public record ProcessedDocuments(Integer count) {

    public static final ProcessedDocuments NONE = new ProcessedDocuments(null);
    public static final ProcessedDocuments ONE = new ProcessedDocuments(1);

    public static ProcessedDocuments in(CustomMessage customMessage) {
        int count = customMessage.getMessageItems().size() + customMessage.getGuarantorItems().size();
        return new ProcessedDocuments(count);
    }

}
