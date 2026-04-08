package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {Names.class})
class NamesTest {

    @MockitoBean
    private TenantCommonRepository tenantRepository;
    @MockitoBean
    private TenantMapper tenantMapper;
    @MockitoBean
    private ApartmentSharingService apartmentSharingService;
    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private TenantStatusService tenantStatusService;
    @MockitoBean
    private ClientAuthenticationFacade clientAuthenticationFacade;
    @MockitoBean
    private DocumentIAService documentIAService;

    @Autowired
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
        verify(documentIAService, never()).analyseDocument(any(Document.class));
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
        verify(documentIAService, never()).analyseDocument(any(Document.class));
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
        verify(documentIAService, never()).analyseDocument(any(Document.class));
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_reset_documents_when_firstname_changes() {
        Document document = new Document();
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>(List.of(document)))
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

        var inOrder = inOrder(tenantRepository, documentIAService);
        inOrder.verify(tenantRepository, times(1)).save(tenant);
        inOrder.verify(documentIAService, times(1)).analyseDocument(document);
    }

    @Test
    void should_reset_documents_when_lastname_changes() {
        Document document = new Document();
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>(List.of(document)))
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
        verify(documentIAService, times(1)).analyseDocument(document);
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_reset_documents_when_preferredname_changes() {
        Document document = new Document();
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>(List.of(document)))
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
        verify(documentIAService, times(1)).analyseDocument(document);
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    void should_reset_and_analyse_guarantor_documents_when_firstname_changes() {
        Document guarantorDocument = new Document();
        Guarantor guarantor = Guarantor.builder()
                .typeGuarantor(TypeGuarantor.LEGAL_PERSON)
                .documents(new ArrayList<>(List.of(guarantorDocument)))
                .build();
        Tenant tenant = Tenant.builder()
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>(List.of(guarantor)))
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
        
        var inOrder = inOrder(tenantRepository, documentIAService);
        inOrder.verify(tenantRepository, times(1)).save(tenant);
        inOrder.verify(documentIAService, times(1)).analyseDocument(guarantorDocument);
    }
}
