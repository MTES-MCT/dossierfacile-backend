package fr.dossierfacile.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class JobContext {

    private final String processId;
    @Setter
    private JobStatus jobStatus;
    private final Long startTime;
    @Setter
    private Long documentId;
    private final Long fileId;
    private final String queueName;

    public JobContext(Long documentId, Long fileId, String queueName) {
        this.processId = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        this.documentId = documentId;
        this.fileId = fileId;
        this.queueName = queueName;
    }

}
