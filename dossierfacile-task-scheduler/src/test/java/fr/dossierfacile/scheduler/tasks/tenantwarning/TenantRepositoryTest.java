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

import static org.junit.jupiter.api.Assertions.assertEquals;


@DataJpaTest
public class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @Sql("/data-two-tenant-same-apart.sql")
    public void testFindCotenantsWithNoEmailAndArchivedMainTenant() {
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "id");
        Page<Tenant> tenantPage = tenantRepository.findCotenantsWithNoEmailAndArchivedMainTenant(pageable);

        assertEquals(1L, tenantPage.getTotalElements());

    }
}