package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceSearchTest {

    @Mock
    private TenantCommonRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void searchesTenantAndGuarantorEmailsAndPreservesPagination() {
        PageRequest pageable = PageRequest.of(1, 20, Sort.by("id").descending());
        Page<Tenant> expected = new PageImpl<>(List.of(Tenant.builder().id(42L).build()), pageable, 21);
        when(tenantRepository.findByTenantOrGuarantorEmailIgnoreCase("Guarantor@example.com", pageable))
                .thenReturn(expected);

        Page<Tenant> results = tenantService.getTenantByIdOrEmail(" Guarantor@example.com ", pageable);

        assertThat(results).isSameAs(expected);
    }
}
