package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.StatsService;
import fr.dossierfacile.common.entity.Stats;
import fr.dossierfacile.common.repository.StatsRepository;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private static final String VALIDATED_DOSSIER_COUNT = "VALIDATED_DOSSIER_COUNT";
    private static final Long VALIDATED_DOSSIER_COUNT_PRE_MIGRATION = 67174L;
    private final StatsRepository statsRepository;

    @Override
    public String getValidatedDossierCount() {
        try {
            return statsRepository.findByKey(VALIDATED_DOSSIER_COUNT).get().getValue();
        } catch (Exception e) {
            log.error("Unavailable Stats - " + VALIDATED_DOSSIER_COUNT);
            Sentry.captureMessage("Unavailable Stats - " + VALIDATED_DOSSIER_COUNT);
        }
        return null;
    }

    @Override
    public void updateStats() {
        Stats processedDossierCount = statsRepository.findByKey(VALIDATED_DOSSIER_COUNT)
                .orElse(Stats.builder().key(VALIDATED_DOSSIER_COUNT).build());
        processedDossierCount.setValue(Long.toString(VALIDATED_DOSSIER_COUNT_PRE_MIGRATION + statsRepository.countValidatedDossier()));
        statsRepository.save(processedDossierCount);
    }
}
