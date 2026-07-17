package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResolverDomainServiceTest {

    private TenantResolverDomainService service;

    @Mock
    private JpaTenantRepository jpaTenantRepository;

    @BeforeEach
    void setUp() {
        service = new TenantResolverDomainService(jpaTenantRepository);
    }

    @Test
    void should_return_current_tenant_when_document_tenant_id_matches_current_tenant_id() {
        TenantEntity tenantEntity = TenantEntity.builder().id(1L).build();
        Tenant currentTenant = new Tenant(tenantEntity);

        DocumentEntity documentEntity = DocumentEntity.builder().tenantId(1L).build();
        Document document = new Document(documentEntity);

        Tenant result = service.resolveTargetedTenant(document, currentTenant);

        assertThat(result).isSameAs(currentTenant);
    }

    @Test
    void should_find_by_guarantor_id_when_document_tenant_id_is_null() {
        DocumentEntity documentEntity = DocumentEntity.builder().tenantId(null).guarantorId(99L).build();
        Document document = new Document(documentEntity);

        TenantEntity targetTenantEntity = TenantEntity.builder().id(2L).build();
        Tenant targetTenant = new Tenant(targetTenantEntity);

        when(jpaTenantRepository.findByGuarantorId(99L)).thenReturn(Optional.of(targetTenant));

        Tenant result = service.resolveTargetedTenant(document);

        assertThat(result).isEqualTo(targetTenant);
    }

    @Test
    void should_find_by_tenant_id_when_document_tenant_id_differs_from_current_tenant() {
        TenantEntity currentTenantEntity = TenantEntity.builder().id(1L).build();
        Tenant currentTenant = new Tenant(currentTenantEntity);

        DocumentEntity documentEntity = DocumentEntity.builder().tenantId(2L).build();
        Document document = new Document(documentEntity);

        TenantEntity targetTenantEntity = TenantEntity.builder().id(2L).build();
        Tenant targetTenant = new Tenant(targetTenantEntity);

        when(jpaTenantRepository.findById(2L)).thenReturn(Optional.of(targetTenant));

        Tenant result = service.resolveTargetedTenant(document, currentTenant);

        assertThat(result).isEqualTo(targetTenant);
    }

    @Test
    void should_throw_exception_when_guarantor_tenant_not_found() {
        DocumentEntity documentEntity = DocumentEntity.builder().tenantId(null).guarantorId(99L).build();
        Document document = new Document(documentEntity);

        when(jpaTenantRepository.findByGuarantorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveTargetedTenant(document))
                .isInstanceOf(ModelNotFoundException.class);
    }

    @Test
    void should_throw_exception_when_tenant_id_not_found() {
        DocumentEntity documentEntity = DocumentEntity.builder().tenantId(2L).build();
        Document document = new Document(documentEntity);

        when(jpaTenantRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveTargetedTenant(document))
                .isInstanceOf(ModelNotFoundException.class);
    }
}
