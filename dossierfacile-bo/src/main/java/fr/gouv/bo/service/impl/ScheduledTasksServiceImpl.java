package fr.gouv.bo.service.impl;

import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.gouv.bo.service.ScheduledTasksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

//
// TODO Attention cela n'est pas compatible avec plusieurs instance !!!
//
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksServiceImpl implements ScheduledTasksService {
    private final TenantCommonRepository tenantRepository;

    @Override
    @Scheduled(fixedDelayString = "${scheduled.process.refresh.rank.delay.minutes}", initialDelayString = "0", timeUnit = TimeUnit.MINUTES)
    public void scheduledRefreshRank() {
        tenantRepository.refreshRank();
    }

}
