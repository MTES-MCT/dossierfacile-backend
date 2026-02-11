package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NamesTest {

    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private ApartmentSharingService apartmentSharingService;
    @Mock
    private DocumentService documentService;
    @Mock
    private TenantStatusService tenantStatusService;
    @Mock
    private ClientAuthenticationFacade clientAuthenticationFacade;

    @InjectMocks
    private Names names;

    @Test
    void should_not_reset_documents_when_names_are_identical() {
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>())
                .apartmentSharing(new ApartmentSharing())
                .build();
        NamesForm namesForm = new NamesForm();
        namesForm.setFirstName("John");
        namesForm.setLastName("Doe");
        namesForm.setPreferredName("Johnny");

        when(tenantStatusService.updateTenantStatus(any(Tenant.class))).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        names.saveStep(tenant, namesForm);

        verify(documentService, never()).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_not_reset_documents_when_tenant_preferred_names_empty() {
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("")
                .documents(new ArrayList<>())
                .apartmentSharing(new ApartmentSharing())
                .build();
        NamesForm namesForm = new NamesForm();
        namesForm.setFirstName("John");
        namesForm.setLastName("Doe");
        namesForm.setPreferredName("");

        when(tenantStatusService.updateTenantStatus(any(Tenant.class))).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        names.saveStep(tenant, namesForm);

        verify(documentService, never()).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_not_reset_documents_when_tenant_preferred_names_undefined() {
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName(null)
                .documents(new ArrayList<>())
                .apartmentSharing(new ApartmentSharing())
                .build();
        NamesForm namesForm = new NamesForm();
        namesForm.setFirstName("John");
        namesForm.setLastName("Doe");
        namesForm.setPreferredName("");

        when(tenantStatusService.updateTenantStatus(any(Tenant.class))).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        names.saveStep(tenant, namesForm);

        verify(documentService, never()).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_reset_documents_when_firstname_changes() {
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>())
                .apartmentSharing(new ApartmentSharing())
                .build();
        NamesForm namesForm = new NamesForm();
        namesForm.setFirstName("Jane");
        namesForm.setLastName("Doe");
        namesForm.setPreferredName("Johnny");

        when(tenantStatusService.updateTenantStatus(any(Tenant.class))).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        names.saveStep(tenant, namesForm);

        verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_reset_documents_when_lastname_changes() {
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>())
                .apartmentSharing(new ApartmentSharing())
                .build();
        NamesForm namesForm = new NamesForm();
        namesForm.setFirstName("John");
        namesForm.setLastName("Smith");
        namesForm.setPreferredName("Johnny");

        when(tenantStatusService.updateTenantStatus(any(Tenant.class))).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        names.saveStep(tenant, namesForm);

        verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_reset_documents_when_preferredname_changes() {
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>())
                .apartmentSharing(new ApartmentSharing())
                .build();
        NamesForm namesForm = new NamesForm();
        namesForm.setFirstName("John");
        namesForm.setLastName("Doe");
        namesForm.setPreferredName("John");

        when(tenantStatusService.updateTenantStatus(any(Tenant.class))).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        names.saveStep(tenant, namesForm);

        verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(tenantRepository, times(1)).save(tenant);
    }
}
