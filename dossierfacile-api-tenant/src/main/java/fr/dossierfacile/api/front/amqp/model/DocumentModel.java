package fr.dossierfacile.api.front.amqp.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentModel {
    Long id;
    Long logId;
}
