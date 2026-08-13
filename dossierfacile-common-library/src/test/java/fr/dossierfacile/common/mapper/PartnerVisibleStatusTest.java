package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerVisibleStatusTest {

    @Test
    void should_mask_completed_as_to_process() {
        assertThat(PartnerVisibleStatus.mask(TenantFileStatus.COMPLETED, "test"))
                .isEqualTo(TenantFileStatus.TO_PROCESS);
    }

    @Test
    void should_keep_every_other_status_unchanged() {
        for (TenantFileStatus status : new TenantFileStatus[]{
                TenantFileStatus.TO_PROCESS, TenantFileStatus.VALIDATED, TenantFileStatus.DECLINED,
                TenantFileStatus.INCOMPLETE, TenantFileStatus.ARCHIVED}) {
            assertThat(PartnerVisibleStatus.mask(status, "test")).isEqualTo(status);
        }
        assertThat(PartnerVisibleStatus.mask(null, "test")).isNull();
    }
}
