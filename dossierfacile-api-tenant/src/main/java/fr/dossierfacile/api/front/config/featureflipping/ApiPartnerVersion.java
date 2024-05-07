package fr.dossierfacile.api.front.config.featureflipping;

import lombok.extern.slf4j.Slf4j;

/**
 * Currently managed versions in application.
 */
@Slf4j
public enum ApiPartnerVersion {
    V3;

    public static ApiPartnerVersion of(int version) {
        try {
            return ApiPartnerVersion.valueOf("V" + version);
        } catch (Exception e) {
            log.error("Try to get a version which doesn't exist");
        }
        return null;
    }

}
