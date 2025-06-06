package fr.dossierfacile.scheduler.tasks.ademe;

import fr.dossierfacile.common.model.AdemeApiResultModel;
import fr.dossierfacile.common.service.AdemeApiServiceImpl;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static fr.dossierfacile.scheduler.tasks.TaskName.CHECK_API_ADEME;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckAdemeApiTask extends AbstractTask {

    // Used to check if the API is down and log details
    @Scheduled(fixedDelayString = "${scheduled.process.check.api.ademe:10}", initialDelayString = "${scheduled.process.check.api.ademe:10}", timeUnit = TimeUnit.MINUTES)
    public void checkAdemeApi() {
        super.startTask(CHECK_API_ADEME);
        AdemeApiServiceImpl ademeApiService = new AdemeApiServiceImpl();
        
        try {
            AdemeApiResultModel ademeApiResultModel = ademeApiService.getDpeDetails("2392E2001612S");

            if (!ademeApiResultModel.getNumero().equals("2392E2001612S")) {
                log.warn("ADEME API ERROR : Error with number : {}", ademeApiResultModel.getNumero());
            }
            if (!ademeApiResultModel.getDateRealisation().equals("2023-06-14T22:00:00Z")) {
                log.warn("ADEME API ERROR : Error with date : {}", ademeApiResultModel.getDateRealisation());
            }
            
            log.info("ADEME API CHECK : OK");
        } catch (Exception e) {
            log.error("ADEME API ERROR : Failed to check API health", e);
        } finally {
            super.endTask();
        }
    }
}
