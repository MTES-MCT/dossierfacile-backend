package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TenantRepositoryTest {

    private static final Pageable LIMIT = PageRequest.of(0, 10);

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @Sql("/data-two-tenant-same-apart.sql")
    void testFindInactiveTenantsWithoutDocuments() {
        LocalDateTime limitDate = LocalDateTime.now().plusDays(45);

        // Tenant 2 (INCOMPLETE, no documents, warnings=0); tenant 1 is ARCHIVED
        assertThat(tenantRepository.findInactiveTenantsWithoutDocuments(limitDate, Long.MAX_VALUE, LIMIT))
                .extracting(Tenant::getId).containsExactly(2L);
        assertThat(tenantRepository.countInactiveTenantsWithoutDocuments(limitDate)).isEqualTo(1L);

        // Tenant 3 (INCOMPLETE, with documents, warnings=0); tenant 1 is ARCHIVED
        assertThat(tenantRepository.findInactiveTenantsWithDocuments(limitDate, 0, Long.MAX_VALUE, LIMIT))
                .extracting(Tenant::getId).containsExactly(3L);
        assertThat(tenantRepository.countInactiveTenantsWithDocuments(limitDate, 0)).isEqualTo(1L);
    }

    @Test
    @Sql("/data-two-tenant-same-apart.sql")
    void batch_queries_only_return_tenants_below_the_id_cursor_in_descending_order() {
        LocalDateTime limitDate = LocalDateTime.now().plusDays(45);

        assertThat(tenantRepository.findInactiveTenantsWithDocuments(limitDate, 0, 3L, LIMIT)).isEmpty();
        assertThat(tenantRepository.findInactiveTenantsWithoutDocuments(limitDate, 2L, LIMIT)).isEmpty();

        List<Long> ids = tenantRepository.findIdsByLastUpdateDateBefore(limitDate, Long.MAX_VALUE, LIMIT);
        assertThat(ids).containsExactly(3L, 2L, 1L);
        assertThat(tenantRepository.findIdsByLastUpdateDateBefore(limitDate, 3L, PageRequest.of(0, 1)))
                .containsExactly(2L);
        assertThat(tenantRepository.countByLastUpdateDateBefore(limitDate)).isEqualTo(3L);
    }
}
