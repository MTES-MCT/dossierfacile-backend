package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharingLink;

/**
 * Service responsible for managing brute-force protection on apartment sharing links.
 * This service tracks failed attempts and blocks access when the threshold is exceeded.
 */
public interface BruteForceProtectionService {

    /**
     * Checks if the link is currently blocked due to too many failed attempts.
     * If the blocking period has expired, it automatically resets the counters.
     *
     * @param link the apartment sharing link to check
     * @throws fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException if the link is blocked
     */
    void checkAndEnforceProtection(ApartmentSharingLink link);

    /**
     * Records a failed trigram validation attempt for the given link.
     * Automatically resets counters if the time window has expired.
     *
     * @param link the apartment sharing link where the attempt failed
     */
    void recordFailedAttempt(ApartmentSharingLink link);

    /**
     * Resets all failed attempt counters for the given link.
     * This is called when a successful trigram validation occurs.
     *
     * @param link the apartment sharing link to reset
     */
    void resetAttempts(ApartmentSharingLink link);

    /**
     * Checks if the time window since the first failed attempt has expired.
     *
     * @param link the apartment sharing link to check
     * @return true if the window has expired (≥1 hour), false otherwise
     */
    boolean hasTimeWindowExpired(ApartmentSharingLink link);
}



