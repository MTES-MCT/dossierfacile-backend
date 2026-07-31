package fr.dossierfacile.api.front.domain.policy;

import fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Pure decision: is this link temporarily blocked by the brute-force protection?
 * Receives the already-loaded link — no repository access (ADR access policy rules).
 *
 * An expired time window means "not blocked": the stale counters are NOT reset here
 * (a policy never writes); they are lazily reinitialized by the next write
 * (BruteForceProtectionService#recordFailedAttempt) or cleared on success (resetAttempts).
 */
@Slf4j
@Component
public class LinkBruteForcePolicy {

    private final int maxFailedAttempts;
    private final long timeWindowHours;

    public LinkBruteForcePolicy(
            @Value("${brute-force.max-attempts:3}") int maxFailedAttempts,
            @Value("${brute-force.time-window-hours:1}") long timeWindowHours) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.timeWindowHours = timeWindowHours;
    }

    public void checkNotBlocked(ApartmentSharingLink link) {
        // No failed attempts recorded - link is accessible
        if (link.getFirstFailedAttemptAt() == null) {
            return;
        }

        // Expired window: attempts are stale, link is accessible again
        if (hasTimeWindowExpired(link)) {
            return;
        }

        if (link.getFailedAttemptCount() != null && link.getFailedAttemptCount() >= maxFailedAttempts) {
            log.warn("Access denied for link [{}] - Too many failed attempts. Count: {}, First attempt: {}",
                    link.getToken(), link.getFailedAttemptCount(), link.getFirstFailedAttemptAt());
            throw new ApplicationLinkBlockedException("Too many failed attempts. Link is temporarily blocked.");
        }
    }

    private boolean hasTimeWindowExpired(ApartmentSharingLink link) {
        long hoursSinceFirstAttempt = ChronoUnit.HOURS.between(link.getFirstFailedAttemptAt(), LocalDateTime.now());
        return hoursSinceFirstAttempt >= timeWindowHours;
    }
}
