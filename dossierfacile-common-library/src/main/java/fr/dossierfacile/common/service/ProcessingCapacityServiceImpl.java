package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ProcessingCapacity;
import fr.dossierfacile.common.repository.ProcessingCapacityRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.service.interfaces.ProcessingCapacityService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ProcessingCapacityServiceImpl implements ProcessingCapacityService {
    private static final LocalTime WORKING_TIME_BEGIN = LocalTime.of(7, 30);
    private static final LocalTime WORKING_TIME_END = LocalTime.of(22, 30);
    private final ProcessingCapacityRepository processingCapacityRepository;
    private final TenantLogRepository tenantLogRepository;
    private final TenantCommonService tenantService;

    @Override
    public LocalDateTime getExpectedProcessingTime(Long tenantId) {

        Long tenantRank = tenantService.getTenantRank(tenantId);
        if (tenantRank == null) {
            return null;
        }
        LocalDate processingDate = LocalDate.now();
        LocalTime processingHour = LocalTime.now();

        // Get processingCapacity for today
        ProcessingCapacity processingCapacity = processingCapacityRepository.findByDate(processingDate);
        if (processingCapacity == null) {
            return null;
        }
        long dailyRemaining = processingCapacity.getDailyCount() - tenantLogRepository.countProcessedDossiersFromToday();

        // Find the processing day
        while (tenantRank > dailyRemaining) {
            tenantRank -= dailyRemaining;
            // compute remaining for the current day
            processingHour = WORKING_TIME_BEGIN;
            processingDate = processingDate.plusDays(1);
            processingCapacity = processingCapacityRepository.findByDate(processingDate);
            if (processingCapacity == null) {
                return null;
            }
            dailyRemaining = processingCapacity.getDailyCount();
        }
        //Find the processing time - in the day
        Duration remainingTime = Duration.between(processingHour, WORKING_TIME_END);
        long minutesRemaining = remainingTime.toMinutes();
        double timeByProcess = (double) minutesRemaining / dailyRemaining;

        return LocalDateTime.of(processingDate, processingHour).plusMinutes((long) (timeByProcess * tenantRank));
    }
}
