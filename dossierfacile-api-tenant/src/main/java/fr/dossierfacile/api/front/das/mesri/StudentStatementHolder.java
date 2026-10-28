package fr.dossierfacile.api.front.das.mesri;

import java.time.LocalDate;

/**
 * Person the statement is issued to. Built from the MESR {@code identite} block when present,
 * otherwise from the tenant's FranceConnect identity (same source of truth, certified by FranceConnect).
 *
 * @param fullName  display name, e.g. "Angela Claire Louise DUBOIS"
 * @param birthDate may be null when neither the MESR nor FranceConnect provided it
 */
public record StudentStatementHolder(String fullName, LocalDate birthDate) {
}
