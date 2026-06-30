package fr.dossierfacile.common.domain.model.tenant;

import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void should_update_last_update_date() {
        TenantEntity entity = TenantEntity.builder().build();
        Tenant tenant = new Tenant(entity);

        assertThat(entity.getLastUpdateDate()).isNull();

        tenant.updateLastUpdateDate();

        assertThat(entity.getLastUpdateDate()).isNotNull();
        assertThat(entity.getLastUpdateDate()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
