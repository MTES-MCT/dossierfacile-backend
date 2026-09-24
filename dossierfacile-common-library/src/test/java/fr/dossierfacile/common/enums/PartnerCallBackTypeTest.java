package fr.dossierfacile.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerCallBackTypeTest {

    @Test
    void forTenantStatus_namesTheEventAfterTheStatus() {
        assertThat(PartnerCallBackType.forTenantStatus(TenantFileStatus.VALIDATED)).isEqualTo(PartnerCallBackType.VERIFIED_ACCOUNT);
        assertThat(PartnerCallBackType.forTenantStatus(TenantFileStatus.COMPLETED)).isEqualTo(PartnerCallBackType.COMPLETED_ACCOUNT);
        assertThat(PartnerCallBackType.forTenantStatus(TenantFileStatus.TO_PROCESS)).isEqualTo(PartnerCallBackType.CREATED_ACCOUNT);
        // Historical behaviour of the manual resends for any other status
        assertThat(PartnerCallBackType.forTenantStatus(TenantFileStatus.INCOMPLETE)).isEqualTo(PartnerCallBackType.CREATED_ACCOUNT);
        assertThat(PartnerCallBackType.forTenantStatus(null)).isEqualTo(PartnerCallBackType.CREATED_ACCOUNT);
    }
}
