package fr.dossierfacile.api.front.domain.policy;

import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.util.TrigramUtils;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Validates that the caller of a full application link knows the trigram of one of the tenants.
 * Receives the already-loaded Tenant aggregates — no repository access (ADR access policy rules).
 * Replica of the legacy ApartmentSharingServiceImpl#validateAndNormalizeTrigram.
 */
@Slf4j
@Component
public class TrigramAccessPolicy {

    public void validateAccess(String trigram, List<Tenant> tenants) {
        if (trigram == null || trigram.isBlank()) {
            log.warn("Missing trigram");
            throw new TrigramNotAuthorizedException("Trigram is required to access full application");
        }

        String normalizedTrigram = trigram.strip().toUpperCase();

        // Valid trigrams come from both the preferred names and the regular names of each tenant
        List<String> validTrigrams = tenants.stream()
                .flatMap(tenant -> Stream.of(
                        tenant.getLastName(),
                        tenant.getPreferredName(),
                        tenant.getUserLastName(),
                        tenant.getUserPreferredName()))
                .map(TrigramUtils::compute)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        boolean trigramMatches = validTrigrams.stream()
                .anyMatch(candidate -> candidate.equalsIgnoreCase(normalizedTrigram));

        if (!trigramMatches) {
            log.warn("Unauthorized trigram [{}]", normalizedTrigram);
            throw new TrigramNotAuthorizedException("Trigram does not match any tenant for this application");
        }
    }
}
