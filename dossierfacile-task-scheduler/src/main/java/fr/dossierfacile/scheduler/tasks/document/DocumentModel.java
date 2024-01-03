package fr.dossierfacile.scheduler.tasks.document;

import lombok.Builder;

@lombok.Value
@Builder
public class DocumentModel {
    Long id;
    Long logId;
}