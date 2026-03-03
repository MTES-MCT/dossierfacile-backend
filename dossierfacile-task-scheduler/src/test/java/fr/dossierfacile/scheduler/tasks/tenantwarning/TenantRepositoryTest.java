package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DataJpaTest
public class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @Sql("/data-two-tenant-same-apart.sql")
    void testFindInactiveTenantsWithoutDocuments() {
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "id");
        LocalDateTime limitDate = LocalDateTime.now().minusDays(45);
        Page<Tenant> tenantPage = tenantRepository.findInactiveTenantsWithoutDocuments(pageable, limitDate);

        // Tenant 2 (INCOMPLETE, no documents, warnings=0) qualifies; tenant 1 is ARCHIVED
        assertEquals(1L, tenantPage.getTotalElements());
    }
}