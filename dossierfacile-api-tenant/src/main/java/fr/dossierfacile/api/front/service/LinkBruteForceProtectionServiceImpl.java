package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Write side of the brute-force protection for apartment sharing links: tracks failed
 * validation attempts within a sliding time window and resets counters on success.
 * The blocked-or-not decision is a pure policy: see LinkBruteForcePolicy.
 */
@Service
@Slf4j
public class LinkBruteForceProtectionServiceImpl implements BruteForceProtectionService {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final long timeWindowHours;

    public LinkBruteForceProtectionServiceImpl(
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            @Value("${brute-force.time-window-hours:1}") long timeWindowHours) {
        this.apartmentSharingLinkRepository = apartmentSharingLinkRepository;
        this.timeWindowHours = timeWindowHours;

        log.info("BruteForceProtectionService initialized with timeWindowHours={}", timeWindowHours);
    }

    // REQUIRES_NEW: this security counter must be committed even when the calling use case
    // transaction rolls back (e.g. the 403 thrown right after recording the attempt).
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(ApartmentSharingLink link) {
        LocalDateTime now = LocalDateTime.now();

        // Stale attempts (expired window) restart the tracking instead of incrementing:
        // the policy treats an expired window as "not blocked" and never writes the reset.
        if (link.getFirstFailedAttemptAt() == null || hasTimeWindowExpired(link)) {
            initializeFailedAttemptTracking(link, now);
        } else {
            incrementFailedAttemptCount(link);
        }

        apartmentSharingLinkRepository.save(link);

        log.info("Failed attempt recorded for link [{}]. Total attempts: {}, First attempt at: {}", 
                link.getToken(), link.getFailedAttemptCount(), link.getFirstFailedAttemptAt());
    }

    // REQUIRES_NEW for symmetry with recordFailedAttempt
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    private boolean hasTimeWindowExpired(ApartmentSharingLink link) {
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



