package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.NameGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NameGuarantorNaturalPerson.class})
class NameGuarantorNaturalPersonTest {

    @MockitoBean
    private TenantCommonRepository tenantRepository;
    @MockitoBean
    private TenantMapper tenantMapper;
    @MockitoBean
    private GuarantorRepository guarantorRepository;
    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private TenantStatusService tenantStatusService;
    @MockitoBean
    private ApartmentSharingService apartmentSharingService;
    @MockitoBean
    private ClientAuthenticationFacade clientAuthenticationFacade;
    @MockitoBean
    private DocumentIAService documentIAService;

    @Autowired
    private NameGuarantorNaturalPerson nameGuarantorNaturalPerson;

    private Tenant buildTenantWithGuarantor(Guarantor guarantor) {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Roe")
                .guarantors(new ArrayList<>(List.of(guarantor)))
                .apartmentSharing(new ApartmentSharing())
                .build();
        guarantor.setTenant(tenant);
        when(tenantRepository.findOneById(1L)).thenReturn(tenant);
        when(guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, guarantor.getId()))
                .thenReturn(Optional.of(guarantor));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        return tenant;
    }

    private NameGuarantorNaturalPersonForm form(Long guarantorId, String first, String last, String preferred) {
        NameGuarantorNaturalPersonForm form = new NameGuarantorNaturalPersonForm();
        form.setGuarantorId(guarantorId);
        form.setFirstName(first);
        form.setLastName(last);
        form.setPreferredName(preferred);
        return form;
    }

    @Test
    void should_not_reset_documents_when_names_are_identical() {
        Guarantor guarantor = Guarantor.builder()
                .id(10L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>(List.of(new Document())))
                .build();
        Tenant tenant = buildTenantWithGuarantor(guarantor);

        nameGuarantorNaturalPerson.saveStep(tenant, form(10L, "John", "Doe", "Johnny"));

        verify(documentService, never()).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(documentIAService, never()).analyseDocument(any(Document.class));
        verify(guarantorRepository, times(1)).save(guarantor);
    }

    // the front sends preferredName="" when the field is left empty, while the DB stores null.
    // This test verifies that preferredName="" is not considered an identity change
    @Test
    void should_not_reset_documents_when_null_preferred_name_matches_empty_form_value() {
        Guarantor guarantor = Guarantor.builder()
                .id(11L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("John")
                .lastName("Doe")
                .preferredName(null)
                .documents(new ArrayList<>(List.of(new Document())))
                .build();
        Tenant tenant = buildTenantWithGuarantor(guarantor);

        nameGuarantorNaturalPerson.saveStep(tenant, form(11L, "John", "Doe", ""));

        verify(documentService, never()).resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(documentIAService, never()).analyseDocument(any(Document.class));
    }

    @Test
    void should_reset_documents_when_first_name_changes() {
        Document document = new Document();
        Guarantor guarantor = Guarantor.builder()
                .id(12L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("John")
                .lastName("Doe")
                .preferredName("Johnny")
                .documents(new ArrayList<>(List.of(document)))
                .build();
        Tenant tenant = buildTenantWithGuarantor(guarantor);

        nameGuarantorNaturalPerson.saveStep(tenant, form(12L, "Jack", "Doe", "Johnny"));

        verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(eq(guarantor.getDocuments()), anyList());
        verify(documentIAService, times(1)).analyseDocument(document);
    }

    @Test
    void should_reset_documents_when_preferred_name_changes() {
        Document document = new Document();
        Guarantor guarantor = Guarantor.builder()
                .id(13L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("John")
                .lastName("Doe")
                .preferredName(null)
                .documents(new ArrayList<>(List.of(document)))
                .build();
        Tenant tenant = buildTenantWithGuarantor(guarantor);

        nameGuarantorNaturalPerson.saveStep(tenant, form(13L, "John", "Doe", "Durand"));

        verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(eq(guarantor.getDocuments()), anyList());
        verify(documentIAService, times(1)).analyseDocument(document);
    }

    @Test
    void should_persist_preferred_name_trimmed() {
        Guarantor guarantor = Guarantor.builder()
                .id(14L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("John")
                .lastName("Doe")
                .preferredName(null)
                .documents(new ArrayList<>())
                .build();
        Tenant tenant = buildTenantWithGuarantor(guarantor);

        nameGuarantorNaturalPerson.saveStep(tenant, form(14L, "John", "Doe", "  Durand  "));

        assertThat(guarantor.getPreferredName()).isEqualTo("Durand");
    }
}
