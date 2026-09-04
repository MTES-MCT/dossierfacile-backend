package fr.dossierfacile.common.enums;

/**
 * Lottery state exposed to the tenant frontend (never to partners); absent when
 * the flag is off or nothing is ongoing — the frontend never knows the flag.
 */
public enum LotteryPublicStatus {
    PENDING,
    DRAWN,
    COOLDOWN
}
