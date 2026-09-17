package fr.dossierfacile.scheduler.cli;

import fr.dossierfacile.logging.task.LogAggregator;
import fr.dossierfacile.logging.util.LoggerUtil;
import fr.dossierfacile.scheduler.AnalyticsReplicationApplication;
import fr.dossierfacile.scheduler.tasks.TaskName;
import fr.dossierfacile.scheduler.tasks.analytics.AnalyticsReplicationService;
import fr.dossierfacile.scheduler.tasks.analytics.DbtTriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Consumer;

@Component
@Slf4j
public class AnalyticsReplicationRunner implements CommandLineRunner {

    private final AnalyticsReplicationService replicationService;
    private final DbtTriggerService dbtTriggerService;
    private final LogAggregator logAggregator;
    private Consumer<Integer> exitHandler = System::exit;

    public AnalyticsReplicationRunner(
            AnalyticsReplicationService replicationService,
            DbtTriggerService dbtTriggerService,
            @Autowired(required = false) LogAggregator logAggregator
    ) {
        this.replicationService = replicationService;
        this.dbtTriggerService = dbtTriggerService;
        this.logAggregator = logAggregator;
    }

    void setExitHandler(Consumer<Integer> exitHandler) {
        this.exitHandler = exitHandler;
    }

    @Override
    public void run(String... args) {
        boolean shouldRun = Arrays.asList(args).contains("--run-task=replicate-analytics")
                || AnalyticsReplicationApplication.class.getName().equals(System.getProperty("loader.main"));
        if (!shouldRun) {
            return;
        }

        LoggerUtil.prepareMDCForScheduledTask(TaskName.REPLICATE_ANALYTICS.name());
        log.info("Démarrage du job de réplication analytics (One-off container)");

        try {
            replicationService.replicateAll();
            dbtTriggerService.trigger();
            log.info("Job de réplication et déclenchement dbt terminés avec succès");
            if (logAggregator != null) {
                logAggregator.sendLogs();
            }
            exitHandler.accept(0);
        } catch (Throwable t) {
            log.error("Échec critique du job de réplication analytics", t);
            if (logAggregator != null) {
                logAggregator.sendLogs();
            }
            exitHandler.accept(1);
        }
    }
}
