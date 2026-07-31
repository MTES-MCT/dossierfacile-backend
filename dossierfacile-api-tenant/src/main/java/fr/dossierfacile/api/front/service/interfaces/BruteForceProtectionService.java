package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharingLink;

/**
 * State mutations of the brute-force protection counters on apartment sharing links.
 * The blocked-or-not decision is a pure policy: see LinkBruteForcePolicy.
 */
public interface BruteForceProtectionService {

    /**
     * Records a failed trigram validation attempt for the given link.
     * If the time window since the first failed attempt has expired, the tracking is
     * reinitialized (count restarts at 1 with a fresh window) instead of incremented.
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
}
