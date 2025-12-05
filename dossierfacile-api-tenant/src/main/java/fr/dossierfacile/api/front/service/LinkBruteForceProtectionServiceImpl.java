package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Implementation of brute-force protection for apartment sharing links.
 * 
 * This service implements a time-window based rate limiting strategy:
 * - Tracks failed validation attempts within a sliding time window
 * - Blocks access when the threshold is exceeded
 * - Automatically resets counters after the time window expires
 * - Resets counters immediately on successful validation
 */
@Service
@Slf4j
public class LinkBruteForceProtectionServiceImpl implements BruteForceProtectionService {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final int maxFailedAttempts;
    private final long timeWindowHours;

    public LinkBruteForceProtectionServiceImpl(
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            @Value("${brute-force.max-attempts:3}") int maxFailedAttempts,
            @Value("${brute-force.time-window-hours:1}") long timeWindowHours) {
        this.apartmentSharingLinkRepository = apartmentSharingLinkRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.timeWindowHours = timeWindowHours;
        
        log.info("BruteForceProtectionService initialized with maxAttempts={}, timeWindowHours={}", 
                maxFailedAttempts, timeWindowHours);
    }

    @Override
    public void checkAndEnforceProtection(ApartmentSharingLink link) {
        // No failed attempts recorded - link is accessible
        if (link.getFirstFailedAttemptAt() == null) {
            return;
        }

        // Check if time window has expired
        if (hasTimeWindowExpired(link)) {
            log.debug("Time window expired for link [{}], resetting counters", link.getToken());
            resetAttempts(link);
            return;
        }

        // Check if link is blocked
        if (isBlocked(link)) {
            log.warn("Access denied for link [{}] - Too many failed attempts. Count: {}, First attempt: {}", 
                    link.getToken(), link.getFailedAttemptCount(), link.getFirstFailedAttemptAt());
            throw new ApplicationLinkBlockedException("Too many failed attempts. Link is temporarily blocked.");
        }
    }

    @Override
    public void recordFailedAttempt(ApartmentSharingLink link) {
        LocalDateTime now = LocalDateTime.now();

        // Initialize or increment counter
        if (link.getFirstFailedAttemptAt() == null) {
            initializeFailedAttemptTracking(link, now);
        } else {
            incrementFailedAttemptCount(link);
        }

        apartmentSharingLinkRepository.save(link);

        log.info("Failed attempt recorded for link [{}]. Total attempts: {}, First attempt at: {}", 
                link.getToken(), link.getFailedAttemptCount(), link.getFirstFailedAttemptAt());
    }

    @Override
    public void resetAttempts(ApartmentSharingLink link) {
        if (link.getFailedAttemptCount() == null && link.getFirstFailedAttemptAt() == null) {
            // Already reset, no need to save
            return;
        }

        link.setFailedAttemptCount(0);
        link.setFirstFailedAttemptAt(null);
        apartmentSharingLinkRepository.save(link);
        
        log.debug("Failed attempts reset for link [{}]", link.getToken());
    }

    @Override
    public boolean hasTimeWindowExpired(ApartmentSharingLink link) {
        if (link.getFirstFailedAttemptAt() == null) {
            return false;
        }

        long hoursSinceFirstAttempt = ChronoUnit.HOURS.between(
                link.getFirstFailedAttemptAt(), 
                LocalDateTime.now()
        );

        return hoursSinceFirstAttempt >= timeWindowHours;
    }

    /**
     * Checks if the link is blocked based on the number of failed attempts.
     */
    private boolean isBlocked(ApartmentSharingLink link) {
        return link.getFailedAttemptCount() != null 
                && link.getFailedAttemptCount() >= maxFailedAttempts;
    }

    /**
     * Initializes failed attempt tracking for a link.
     */
    private void initializeFailedAttemptTracking(ApartmentSharingLink link, LocalDateTime now) {
        link.setFirstFailedAttemptAt(now);
        link.setFailedAttemptCount(1);
        
        log.debug("Initialized failed attempt tracking for link [{}] at {}", link.getToken(), now);
    }

    /**
     * Increments the failed attempt counter for a link.
     */
    private void incrementFailedAttemptCount(ApartmentSharingLink link) {
        int currentCount = link.getFailedAttemptCount() != null ? link.getFailedAttemptCount() : 0;
        link.setFailedAttemptCount(currentCount + 1);
    }
}



