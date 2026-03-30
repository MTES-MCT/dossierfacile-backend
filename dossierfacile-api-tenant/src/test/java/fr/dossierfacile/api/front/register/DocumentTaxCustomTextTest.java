package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentTaxGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.tenant.DocumentTax;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 *
 * This test demonstrates inconsistency between tenant and guarantor handling of customText
 * for OTHER_TAX documents when noDocument=false (i.e. files are provided).
 * How to reproduce in frontend: "Pas d'avis d'imposition" > "Autre Situation"
 * Tenant: saves customText regardless of noDocument flag.
 * Guarantor: only saves customText when noDocument=true — loses it when files are present.
 */
@ExtendWith(MockitoExtension.class)
class DocumentTaxCustomTextTest {

    private static final String EXPLANATION = "J'ai vécu à l'étranger et je n'ai pas d'avis d'imposition français.";

    private static Document invokeSaveDocument(Object service, Tenant tenant, Object form) throws Exception {
        Method method = service.getClass().getDeclaredMethod("saveDocument", Tenant.class, form.getClass());
        method.setAccessible(true);
        return (Document) method.invoke(service, tenant, form);
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class TenantTax {

        @Mock
        DocumentHelperService documentHelperService;
        @Mock
        TenantCommonRepository tenantRepository;
        @Mock
        DocumentRepository documentRepository;
        @Mock
        DocumentService documentService;
        @Mock
        TenantStatusService tenantStatusService;
        @Mock
        ApartmentSharingService apartmentSharingService;

        @InjectMocks
        DocumentTax documentTax;

        private Tenant tenant;

        @BeforeEach
        void setUp() {
            var apartmentSharing = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
            tenant = Tenant.builder().id(1L).apartmentSharing(apartmentSharing).documents(new ArrayList<>()).build();
            apartmentSharing.setTenants(new ArrayList<>());

            when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), any()))
                    .thenReturn(Optional.empty());
            when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(tenantRepository.save(any())).thenReturn(tenant);
        }

        @Test
        void shouldSaveCustomTextForOtherTaxWithFiles() throws Exception {
            var form = new DocumentTaxForm();
            form.setTypeDocumentTax(DocumentSubCategory.OTHER_TAX);
            form.setNoDocument(false);
            form.setCustomText(EXPLANATION);
            form.setDocuments(Collections.emptyList());

            var captor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            invokeSaveDocument(documentTax, tenant, form);

            Document saved = captor.getValue();
            assertThat(saved.getCustomText()).isEqualTo(EXPLANATION);
            assertThat(saved.getNoDocument()).isFalse();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GuarantorTax {

        @Mock
        DocumentHelperService documentHelperService;
        @Mock
        TenantCommonRepository tenantRepository;
        @Mock
        DocumentRepository documentRepository;
        @Mock
        GuarantorRepository guarantorRepository;
        @Mock
        DocumentService documentService;
        @Mock
        TenantStatusService tenantStatusService;
        @Mock
        ApartmentSharingService apartmentSharingService;

        @InjectMocks
        DocumentTaxGuarantorNaturalPerson documentTaxGuarantor;

        private Tenant tenant;

        @BeforeEach
        void setUp() {
            var apartmentSharing = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
            tenant = Tenant.builder().id(1L).apartmentSharing(apartmentSharing).documents(new ArrayList<>()).build();
            apartmentSharing.setTenants(new ArrayList<>());

            var guarantor = Guarantor.builder().id(10L).tenant(tenant).typeGuarantor(TypeGuarantor.NATURAL_PERSON).documents(new ArrayList<>()).build();

            when(guarantorRepository.findByTenantAndTypeGuarantorAndId(any(), eq(TypeGuarantor.NATURAL_PERSON), eq(10L)))
                    .thenReturn(Optional.of(guarantor));
            when(documentRepository.findFirstByDocumentCategoryAndGuarantor(eq(DocumentCategory.TAX), any()))
                    .thenReturn(Optional.empty());
            when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(tenantRepository.save(any())).thenReturn(tenant);
        }

        @Test
        void shouldSaveCustomTextForOtherTaxWithFiles() throws Exception {
            var form = new DocumentTaxGuarantorNaturalPersonForm();
            form.setTypeDocumentTax(DocumentSubCategory.OTHER_TAX);
            form.setNoDocument(false);
            form.setCustomText(EXPLANATION);
            form.setGuarantorId(10L);
            form.setDocuments(Collections.emptyList());

            var captor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            invokeSaveDocument(documentTaxGuarantor, tenant, form);

            Document saved = captor.getValue();
            // guarantor loses customText when noDocument=false, unlike tenant
            assertThat(saved.getCustomText()).isNull();
            assertThat(saved.getNoDocument()).isFalse();
        }
    }
}
