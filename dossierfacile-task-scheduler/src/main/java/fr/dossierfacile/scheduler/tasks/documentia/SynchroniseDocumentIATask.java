package fr.dossierfacile.scheduler.tasks.documentia;

import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynchroniseDocumentIATask extends AbstractTask {

    /**
     * This task synchronises the status of Document IA analyses that are in the STARTED state.
     * It retrieves a batch of analyses with the status STARTED from the database and checks their
     * status with the Document IA service. The task runs every 10 seconds and processes analyses
     * in batches of 100, using virtual threads to handle multiple analyses concurrently while
     * limiting the number of active threads to avoid overwhelming the Document IA service.
     **/

    private static final int BATCH_SIZE = 100;
    private final Semaphore threadLimiter = new Semaphore(10);

    private final DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    private final DocumentIAService documentIaService;

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void synchroniseDocumentIA() {
        super.startTask(TaskName.SYNCHRONISE_DOCUMENT_IA);

        Pageable pageable = PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "creationDate"));

        var analyses = documentIAFileAnalysisRepository.findAllByAnalysisStatus(
                DocumentIAFileAnalysisStatus.STARTED,
                pageable
        );

        log.info("Found {} analyses to synchronise with Document IA", analyses.size());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var analysis : analyses) {
                executor.submit(() -> {
                    try {
                        threadLimiter.acquire();
                        try {
                            log.info("Starting synchronise with Document IA for analysis id={}", analysis.getId());
                            documentIaService.checkAnalysisStatus(analysis);
                            log.info("Finished synchronise with Document IA for analysis id={}", analysis.getId());
                        } finally {
                            threadLimiter.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Thread interrompu pour l'analyse id={}", analysis.getId());
                    }
                });
            }
        }
        super.endTask();
    }

}
