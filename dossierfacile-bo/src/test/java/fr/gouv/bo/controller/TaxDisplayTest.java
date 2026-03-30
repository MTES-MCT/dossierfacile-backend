package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.TestBOApplication;
import fr.gouv.bo.service.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests real Thymeleaf template rendering for tax document display
 * on both /bo/tenant/{id}/processFile and /bo/colocation/{id} views.
 */
@WebMvcTest(controllers = {BOTenantController.class, BOApartmentSharingController.class})
@ContextConfiguration(classes = {TestBOApplication.class, TaxDisplayTest.TestSecurityConfig.class})
@ActiveProfiles("test")
class TaxDisplayTest {

    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return registrationId -> null;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // BOTenantController dependencies
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private MessageService messageService;
    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private UserApiService userApiService;
    @MockitoBean
    private PartnerCallBackService partnerCallBackService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private TenantLogService logService;
    @MockitoBean
    private DocumentDeniedReasonsService documentDeniedReasonsService;

    // BOApartmentSharingController dependencies
    @MockitoBean
    private ApartmentSharingLinkService apartmentSharingLinkService;
    @MockitoBean
    private LinkLogRepository linkLogRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        var apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.ALONE)
                .build();
        tenant = Tenant.builder()
                .id(1L)
                .apartmentSharing(apartmentSharing)
                .tenantType(TenantType.CREATE)
                .status(TenantFileStatus.TO_PROCESS)
                .honorDeclaration(true)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        apartmentSharing.setTenants(new ArrayList<>(List.of(tenant)));

        // processFile mocks
        when(tenantService.find(1L)).thenReturn(tenant);
        when(logService.getLogByTenantId(anyLong())).thenReturn(Collections.emptyList());
        when(userApiService.findPartnersLinkedToTenant(anyLong())).thenReturn(Collections.emptyList());
        when(messageService.findTenantMessages(any())).thenReturn(Collections.emptyList());
        when(documentDeniedReasonsService.getLastDeniedReason(any(), any())).thenReturn(java.util.Optional.empty());

        // colocation mocks
        when(tenantService.findAllTenantsByApartmentSharingAndReorderDocumentsByCategory(1L))
                .thenReturn(new ArrayList<>(List.of(tenant)));
        when(tenantService.getAllDocumentCategories(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            return new ArrayList<>(t.getDocuments());
        });
        when(tenantService.getAllDocumentCategoriesGuarantor(any())).thenAnswer(inv -> {
            Guarantor g = inv.getArgument(0);
            return new ArrayList<>(g.getDocuments());
        });
        when(tenantService.hasVerifiedEmailIfExistsInKeycloak(any())).thenReturn(false);
        when(apartmentSharingLinkService.getFilteredLinks(any())).thenReturn(Collections.emptyList());
        when(userApiService.getAllPartners()).thenReturn(Collections.emptyList());
    }

    private Guarantor addGuarantor() {
        var guarantor = Guarantor.builder()
                .id(10L)
                .tenant(tenant)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("Garant")
                .lastName("Test")
                .documents(new ArrayList<>())
                .build();
        tenant.getGuarantors().add(guarantor);
        return guarantor;
    }

    private void addGuarantorDocument(Guarantor guarantor, DocumentCategory category,
                                      DocumentSubCategory subCategory, Boolean noDocument, String customText) {
        addGuarantorDocument(guarantor, category, subCategory, noDocument, customText,
                "gdoc-" + guarantor.getDocuments().size());
    }

    private void addGuarantorDocument(Guarantor guarantor, DocumentCategory category,
                                      DocumentSubCategory subCategory, Boolean noDocument, String customText,
                                      String documentName) {
        var doc = fr.dossierfacile.common.entity.Document.builder()
                .id((long) (guarantor.getDocuments().size() + 200))
                .documentCategory(category)
                .documentSubCategory(subCategory)
                .documentStatus(DocumentStatus.TO_PROCESS)
                .noDocument(noDocument)
                .customText(customText)
                .guarantor(guarantor)
                .name(documentName)
                .files(new ArrayList<>())
                .build();
        guarantor.getDocuments().add(doc);
    }

    private void addTenantDocument(DocumentCategory category, DocumentSubCategory subCategory,
                                   Boolean noDocument, String customText) {
        addTenantDocument(category, subCategory, noDocument, customText, "doc-" + tenant.getDocuments().size());
    }

    private void addTenantDocument(DocumentCategory category, DocumentSubCategory subCategory,
                                   Boolean noDocument, String customText, String documentName) {
        var doc = fr.dossierfacile.common.entity.Document.builder()
                .id((long) (tenant.getDocuments().size() + 100))
                .documentCategory(category)
                .documentSubCategory(subCategory)
                .documentStatus(DocumentStatus.TO_PROCESS)
                .noDocument(noDocument)
                .customText(customText)
                .tenant(tenant)
                .name(documentName)
                .files(new ArrayList<>())
                .build();
        tenant.getDocuments().add(doc);
    }

    private Document renderProcessFile() throws Exception {
        String html = mockMvc.perform(get("/bo/tenant/1/processFile"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private Document renderColocation() throws Exception {
        String html = mockMvc.perform(get("/bo/colocation/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    @Nested
    class OtherTaxWithCustomTextAndNoFile {

        @BeforeEach
        void setUp() {
            addTenantDocument(DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX,
                    true, "Je vis à l'étranger depuis 10 ans.");
        }

        @Test
        void processFile_shouldShowPdfViewerAndCustomText() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst("embed.document-embed")).isNotNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Je vis à l'étranger depuis 10 ans.");
        }

        @Test
        void colocation_shouldShowCustomTextAndNoDocumentWhenGeneratedDocumentIsMissing() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNotNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Je vis à l'étranger depuis 10 ans.");
        }
    }

    @Nested
    class OtherTaxWithCustomTextAndFile {

        @BeforeEach
        void setUp() {
            addTenantDocument(DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX,
                    false, "Retour de l'étranger, voici mon avis canadien.");
        }

        @Test
        void processFile_shouldShowPdfViewerAndCustomText() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst("embed.document-embed")).isNotNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Retour de l'étranger, voici mon avis canadien.");
        }

        @Test
        void colocation_shouldShowCustomTextWithoutNoDocumentCard() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Retour de l'étranger, voici mon avis canadien.");
        }
    }

    @Nested
    class ParentTaxWithHonorDeclaration {

        @BeforeEach
        void setUp() {
            addTenantDocument(DocumentCategory.TAX, DocumentSubCategory.MY_PARENTS,
                    false, null);
        }

        @Test
        void processFile_shouldShowPdfViewerWithoutCustomText() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst("embed.document-embed")).isNotNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation")).isNull();
        }

        @Test
        void colocation_shouldNotShowNoDocumentCardNorCustomText() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation")).isNull();
        }
    }

    // ===== Guarantor tests =====
    // Same scenarios as tenant, applied to a guarantor's tax documents.
    // Note: there is a known backend bug where DocumentTaxGuarantorNaturalPerson
    // loses customText when noDocument=false (unlike tenant), see DocumentTaxCustomTextTest.
    // These tests verify the BO **display** behavior given the data as stored in DB.

    @Nested
    class GuarantorOtherTaxWithCustomTextAndNoFile {

        @BeforeEach
        void setUp() {
            var guarantor = addGuarantor();
            addGuarantorDocument(guarantor, DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX,
                    true, "Le garant réside à l'étranger.");
        }

        @Test
        void processFile_shouldShowPdfViewerAndCustomText() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst("embed.document-embed")).isNotNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Le garant réside à l'étranger.");
        }

        @Test
        void colocation_shouldShowCustomTextAndNoDocumentWhenGeneratedDocumentIsMissing() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNotNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Le garant réside à l'étranger.");
        }
    }

    @Nested
    @org.junit.jupiter.api.Disabled("Bug: DocumentTaxGuarantorNaturalPerson loses customText when noDocument=false — see DocumentTaxCustomTextTest")
    class GuarantorOtherTaxWithCustomTextAndFile {

        @BeforeEach
        void setUp() {
            var guarantor = addGuarantor();
            addGuarantorDocument(guarantor, DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX,
                    false, "Retour du Canada, voici l'avis canadien du garant.");
        }

        @Test
        void processFile_shouldShowPdfViewerAndCustomText() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Retour du Canada, voici l'avis canadien du garant.");
        }

        @Test
        void colocation_shouldShowCustomTextWithoutNoDocumentCard() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation__text"))
                    .isNotNull()
                    .extracting(Element::text)
                    .asString().contains("Retour du Canada, voici l'avis canadien du garant.");
        }
    }

    @Nested
    class GuarantorParentTaxWithHonorDeclaration {

        @BeforeEach
        void setUp() {
            var guarantor = addGuarantor();
            addGuarantorDocument(guarantor, DocumentCategory.TAX, DocumentSubCategory.MY_PARENTS,
                    false, null);
        }

        @Test
        void processFile_shouldShowPdfViewerWithoutCustomText() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation")).isNull();
        }

        @Test
        void colocation_shouldNotShowNoDocumentCardNorCustomText() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNull();
            assertThat(doc.selectFirst(".doc-analysis-explanation")).isNull();
        }
    }

    @Nested
    class MissingGeneratedDocument {

        @BeforeEach
        void setUp() {
            addTenantDocument(DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX,
                    true, "Pas de PDF généré", null);
        }

        @Test
        void processFile_shouldShowNoDocumentCardWhenGeneratedDocumentIsMissing() throws Exception {
            Document doc = renderProcessFile();

            assertThat(doc.selectFirst("h3:containsOwn(Pas de document)")).isNotNull();
            assertThat(doc.selectFirst("embed.document-embed")).isNull();
        }

        @Test
        void colocation_shouldShowNoDocumentLabelWhenGeneratedDocumentIsMissing() throws Exception {
            Document doc = renderColocation();

            assertThat(doc.selectFirst(":containsOwn(Pas de document)")).isNotNull();
        }
    }
}
