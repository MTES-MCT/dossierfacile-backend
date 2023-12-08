package fr.dossierfacile.scheduler.tasks.documenttoprocess;

import lombok.Builder;

@lombok.Value
@Builder
public class DocumentModel {
    Long id;
    Long logId;
}