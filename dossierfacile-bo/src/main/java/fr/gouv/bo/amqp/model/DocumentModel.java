package fr.gouv.bo.amqp.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentModel {
    Long id;
    Long logId;
}
