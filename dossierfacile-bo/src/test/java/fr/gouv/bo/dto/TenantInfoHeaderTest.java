package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static fr.dossierfacile.common.enums.ApplicationType.ALONE;
import static fr.dossierfacile.common.enums.ApplicationType.COUPLE;
import static fr.dossierfacile.common.enums.LogType.*;
import static fr.gouv.bo.dto.TenantInfoHeader.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TenantInfoHeaderTest {

    @Test
    void should_display_name_and_application_type() {
        TenantInfoHeader header = build(tenant(COUPLE), emptyList(), emptyList());

        assertThat(header.getElements()).containsExactlyElementsOf(
                headerElements(
                        "FranceConnecté", "Non",
                        "Nom", "John Doe",
                        "Dossier", "En couple avec Jane Doe",
                        "Partenaires", "",
                        "Historique", "Premier passage"
                )
        );
    }

    @Test
    void should_display_list_of_partners() {
        List<UserApi> partners = List.of(
                UserApi.builder().name("technical-name2").name2("Pretty Name 1").build(),
                UserApi.builder().name("technical-name2").build()
        );
        TenantInfoHeader header = build(tenant(ALONE), partners, emptyList());

        assertThat(header.getElements()).containsExactlyElementsOf(
                headerElements(
                        "FranceConnecté", "Non",
                        "Nom", "John Doe",
                        "Dossier", "Seul·e",
                        "Partenaires", "Pretty Name 1, technical-name2",
                        "Historique", "Premier passage"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("logsAndExpectedStatus")
    void should_display_times_tenant_appeared_in_bo(List<TenantLog> logs, String expectedStatus) {
        TenantInfoHeader header = build(tenant(ALONE), emptyList(), logs);

        assertThat(header.getElements()).containsExactlyElementsOf(
                headerElements(
                        "FranceConnecté", "Non",
                        "Nom", "John Doe",
                        "Dossier", "Seul·e",
                        "Partenaires", "",
                        "Historique", expectedStatus
                )
        );
    }

    private static Stream<Arguments> logsAndExpectedStatus() {
        return Stream.of(
                arguments(List.of(
                        log(1, ACCOUNT_EDITED),
                        log(2, ACCOUNT_EDITED)
                ), "Premier passage"),
                arguments(List.of(
                        log(1, ACCOUNT_DENIED)
                ), "Refusé 1 fois"),
                arguments(List.of(
                        log(2, ACCOUNT_DENIED),
                        log(1, ACCOUNT_CREATED),
                        log(3, ACCOUNT_VALIDATED)
                ), "Validé il y a 21 heures"),
                arguments(List.of(
                        log(3, ACCOUNT_VALIDATED),
                        log(4, ACCOUNT_DENIED),
                        log(1, ACCOUNT_CREATED),
                        log(2, ACCOUNT_DENIED)
                ), "Refusé 1 fois"),
                arguments(List.of(
                        log(1, ACCOUNT_CREATED),
                        log(2, ACCOUNT_EDITED),
                        log(3, ACCOUNT_COMPLETE),
                        log(4, ACCOUNT_VALIDATED),
                        log(5, ACCOUNT_EDITED),
                        log(6, ACCOUNT_DENIED),
                        log(7, ACCOUNT_EDITED),
                        log(8, ACCOUNT_DENIED)
                ), "Refusé 2 fois")
        );
    }

    private Tenant tenant(ApplicationType applicationType) {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .applicationType(applicationType)
                .tenants(emptyList())
                .build();
        if (applicationType != ALONE) {
            Tenant cotenant = Tenant.builder().id(2L).firstName("Jane").lastName("Doe").build();
            apartmentSharing.setTenants(List.of(cotenant));
        }
        return Tenant.builder().id(1L)
                .firstName("John").lastName("Doe")
                .apartmentSharing(apartmentSharing)
                .build();
    }

    private static TenantLog log(int temporalOrder, LogType logType) {
        TenantLog log = new TenantLog();
        log.setLogType(logType);
        log.setCreationDateTime(LocalDateTime.now().minusDays(1).plusHours(temporalOrder));
        return log;
    }

    private List<HeaderElement> headerElements(String label1, String value1, String label2, String value2,
                                               String label3, String value3, String label4, String value4,
                                               String label5, String value5) {
        return List.of(
                new HeaderElement(label1, value1),
                new HeaderElement(label2, value2),
                new HeaderElement(label3, value3),
                new HeaderElement(label4, value4),
                new HeaderElement(label5, value5)
        );
    }

}