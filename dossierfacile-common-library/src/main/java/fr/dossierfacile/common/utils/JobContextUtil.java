package fr.dossierfacile.common.utils;

import fr.dossierfacile.common.model.JobContext;

import java.util.Map;

import static fr.dossierfacile.logging.util.LoggerUtil.*;

public class JobContextUtil {

    private JobContextUtil() {}

    public static Map<String, String> prepareJobAttributes(JobContext jobContext) {
        return Map.of(
                PROCESS_FILE_ID, String.valueOf(jobContext.getFileId()),
                PROCESS_DOCUMENT_ID, String.valueOf(jobContext.getDocumentId()),
                PROCESS_QUEUE_NAME, jobContext.getQueueName(),
                PROCESS_JOB_STATUS, jobContext.getJobStatus().name()
        );
    }
}
