package fr.gouv.bo.model;

import fr.gouv.bo.dto.CustomMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ProcessedDocuments(Integer count, Integer timeSpent) {

    public static final ProcessedDocuments NONE = new ProcessedDocuments(null, null);
    public static final ProcessedDocuments ONE = new ProcessedDocuments(1, null);

    public static ProcessedDocuments in(CustomMessage customMessage) {
        int countGuarantorDocuments = customMessage.getGuarantorItems().stream()
                .mapToInt(i -> i.getMessageItems().size())
                .sum();
        int count = customMessage.getMessageItems().size() +  countGuarantorDocuments;
        try {
            int timeSpent = Integer.parseInt(customMessage.getTimeSpent()) / 1000;
            return new ProcessedDocuments(count, timeSpent);
        } catch (Exception e) {
            log.error("Unable to parse timeSpent in customMessage ts= " + customMessage.getTimeSpent());
        }
        return new ProcessedDocuments(count, null);
    }

}
