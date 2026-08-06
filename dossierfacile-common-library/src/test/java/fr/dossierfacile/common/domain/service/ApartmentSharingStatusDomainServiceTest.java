package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static fr.dossierfacile.common.enums.TenantFileStatus.ARCHIVED;
import static fr.dossierfacile.common.enums.TenantFileStatus.DECLINED;
import static fr.dossierfacile.common.enums.TenantFileStatus.INCOMPLETE;
import static fr.dossierfacile.common.enums.TenantFileStatus.TO_PROCESS;
import static fr.dossierfacile.common.enums.TenantFileStatus.VALIDATED;
import static org.assertj.core.api.Assertions.assertThat;

class ApartmentSharingStatusDomainServiceTest {

    private final ApartmentSharingStatusDomainService service = new ApartmentSharingStatusDomainService();

    static Stream<Arguments> statusCombinations() {
        return Stream.of(
                // DECLINED wins over everything
                Arguments.of(List.of(DECLINED, VALIDATED), DECLINED),
                Arguments.of(List.of(ARCHIVED, DECLINED), DECLINED),
                Arguments.of(List.of(TO_PROCESS, DECLINED, INCOMPLETE), DECLINED),
                // then INCOMPLETE
                Arguments.of(List.of(INCOMPLETE, VALIDATED), INCOMPLETE),
                Arguments.of(List.of(INCOMPLETE, ARCHIVED), INCOMPLETE),
                Arguments.of(List.of(INCOMPLETE, TO_PROCESS), INCOMPLETE),
                // ARCHIVED: all archived -> ARCHIVED, partially archived -> INCOMPLETE
                Arguments.of(List.of(ARCHIVED, ARCHIVED), ARCHIVED),
                Arguments.of(List.of(ARCHIVED, VALIDATED), INCOMPLETE),
                Arguments.of(List.of(ARCHIVED, TO_PROCESS), INCOMPLETE),
                // then TO_PROCESS
                Arguments.of(List.of(TO_PROCESS, VALIDATED), TO_PROCESS),
                // otherwise VALIDATED
                Arguments.of(List.of(VALIDATED, VALIDATED), VALIDATED),
                Arguments.of(List.of(VALIDATED), VALIDATED)
        );
    }

    @ParameterizedTest
    @MethodSource("statusCombinations")
    void computes_aggregated_status_like_legacy(List<TenantFileStatus> statuses, TenantFileStatus expected) {
        assertThat(service.computeStatus(statuses)).isEqualTo(expected);
    }

    @Test
    void empty_sharing_is_validated_like_legacy() {
        assertThat(service.computeStatus(List.of())).isEqualTo(VALIDATED);
    }

    @Test
    void order_of_tenants_does_not_matter() {
        List<TenantFileStatus> statuses = Arrays.asList(VALIDATED, ARCHIVED, TO_PROCESS);

        assertThat(service.computeStatus(statuses))
                .isEqualTo(service.computeStatus(statuses.reversed()))
                .isEqualTo(INCOMPLETE);
    }
}
