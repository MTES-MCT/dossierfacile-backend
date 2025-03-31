package fr.dossierfacile.api.front.register.tenant;


import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.mapper.VersionedCategoriesMapper;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {Names.class, TenantMapperImpl.class, VersionedCategoriesMapper.class})
@TestPropertySource(properties = {"application.api.version = 4"})
class NameSaveStepTest {

    @Autowired
    Names names;

    @MockBean
    TenantCommonRepository tenantCommonRepository;
    @MockBean
    ApartmentSharingService apartmentSharingService;
    @MockBean
    DocumentService documentService;
    @MockBean
    TenantStatusService tenantStatusService;
    @MockBean
    ClientAuthenticationFacade clientAuthenticationFacade;

    @Test
    @WithMockUser(username = "test", authorities = "SCOPE_dossier")
    void shouldReturnTenantWhenNoNamesChangeWhenOwnerTypeSelf() {
        var apartmentSharing = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
        var tenant = Tenant.builder().id(1L).firstName("firstName").lastName("lastName").ownerType(TenantOwnerType.SELF).apartmentSharing(apartmentSharing).build();

        apartmentSharing.setTenants(List.of(tenant));
        var namesForm = new NamesForm();
        namesForm.setFirstName("firstName");
        namesForm.setLastName("lastName");
        namesForm.setOwnerType(TenantOwnerType.SELF);

        when(tenantCommonRepository.save(any())).thenReturn(tenant);
        var result = names.saveStep(tenant, namesForm);

        assertThat(result.getFirstName()).isEqualTo("firstName");
        assertThat(result.getLastName()).isEqualTo("lastName");

        verify(documentService, times(0)).resetValidatedOrInProgressDocumentsAccordingCategories(any(), any());
    }

    @Test
    @WithMockUser(username = "test", authorities = "SCOPE_dossier")
    void shouldReturnTenantWithNewNamesChange() {
        var apartmentSharing = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
        var tenant = Tenant.builder().id(1L).firstName("firstName").lastName("lastName").ownerType(TenantOwnerType.SELF).apartmentSharing(apartmentSharing).build();

        apartmentSharing.setTenants(List.of(tenant));
        var namesForm = new NamesForm();
        namesForm.setFirstName("test");
        namesForm.setLastName("test");
        namesForm.setOwnerType(TenantOwnerType.SELF);

        when(tenantCommonRepository.save(any())).thenReturn(tenant);
        var result = names.saveStep(tenant, namesForm);

        assertThat(result.getFirstName()).isEqualTo("test");
        assertThat(result.getLastName()).isEqualTo("test");

        verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(any(), any());
    }

}
