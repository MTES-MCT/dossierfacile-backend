package fr.dossierfacile.api.pdfgenerator.amqp.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentModel {
    Long id;
}
