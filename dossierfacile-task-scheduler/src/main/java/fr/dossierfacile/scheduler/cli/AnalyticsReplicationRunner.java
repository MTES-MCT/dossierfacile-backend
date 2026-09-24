package fr.dossierfacile.scheduler.cli;

import fr.dossierfacile.logging.task.LogAggregator;
import fr.dossierfacile.logging.util.LoggerUtil;
import fr.dossierfacile.scheduler.AnalyticsReplicationApplication;
import fr.dossierfacile.scheduler.tasks.TaskName;
import fr.dossierfacile.scheduler.tasks.analytics.AnalyticsProperties;
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

    private final AnalyticsProperties properties;
    private final AnalyticsReplicationService replicationService;
    private final DbtTriggerService dbtTriggerService;
    private final LogAggregator logAggregator;
    private Consumer<Integer> exitHandler = System::exit;

    public AnalyticsReplicationRunner(
            AnalyticsProperties properties,
            AnalyticsReplicationService replicationService,
            DbtTriggerService dbtTriggerService,
            @Autowired(required = false) LogAggregator logAggregator
    ) {
        this.properties = properties;
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

        if (!properties.isEnabled()) {
            log.info("Analytics task skipped. To activate it: analytics.replication.enabled = true");
            exitHandler.accept(0);
            return;
        }

        log.info("Démarrage du job de réplication analytics (One-off container)");

        boolean replicationSucceeded = false;
        try {
            replicationService.replicateAll();
            replicationSucceeded = true;
            log.info("Job de réplication terminé avec succès");
        } catch (Exception e) {
            log.error("Échec critique du job de réplication analytics", e);
        } finally {
            try {
                dbtTriggerService.trigger();
            } catch (Exception e) {
                log.error("Erreur inattendue lors de l'appel au webhook dbt", e);
            }
            if (logAggregator != null) {
                try {
                    logAggregator.sendLogs();
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi des logs", e);
                }
            }
            if (replicationSucceeded) {
                log.info("Job de réplication et déclenchement dbt terminés avec succès");
                exitHandler.accept(0);
            } else {
                exitHandler.accept(1);
            }
        }
    }
}
