package fr.dossierfacile.scheduler.tasks.garbagecollection;

import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GarbageCollectionTask extends AbstractTask {

    private final GarbageCollectionService garbageCollectionService;

    /*
     * Run every minute !
     * The purpose is to delete files from multiAzBucket that have been missed by the usual deletion process.
     * It works by pulling the logs 100 at a time (BATCH_SIZE).
     * The custom request will sequentially pull logs with a start point stored in GarbageSequence table
     * It will pull the next 100 logs after the sequence value.
     * Inside this batch, it will get the distinct tenantIds and return the last status for each of them.
     * After that we will filter only the tenant with the status [LogType.ACCOUNT_DELETE, LogType.ACCOUNT_ARCHIVED, LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS]
     * For each of them we will check in each bucket if there is any file left and delete it if so.
     * If the batch is not empty, we will update the sequence value by adding the batch size.
     * If nothing is returned, it means we have processed all the logs and do nothing.
     * The logs table will grow and the process will resume after a while.
    */
    @Scheduled(cron = "0 * * * * *")
    public void garbageCollectionTask() {
        super.startTask(TaskName.GARBAGE_COLLECTION_V2);
        log.info("Garbage collection task started");
        garbageCollectionService.handleGarbageCollection();
        super.endTask();
    }

}
