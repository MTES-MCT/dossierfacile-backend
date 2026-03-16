package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.model.tenant.CoTenantModel;
import fr.dossierfacile.api.front.model.tenant.FileModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRuleLevel;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static fr.dossierfacile.api.front.mapper.TenantGraphBuilder.aTenant;
import static fr.dossierfacile.api.front.mapper.TenantGraphBuilder.rule;
import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperTest {

    private TenantMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new TenantMapperImpl();
        mapper.applicationBaseUrl = "https://api.example.com";
        mapper.tenantBaseUrl = "https://example.com";
        mapper.minBrokenRulesLevel = DocumentRuleLevel.WARN;

        // Required because @AfterMapping accesses SecurityContextHolder
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new TestingAuthenticationToken("user", null, Collections.emptyList()));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Document buildDocument(String name) {
        StorageFile watermarkFile = new StorageFile();
        watermarkFile.setName("watermark.pdf");
        return Document.builder()
                .name(name)
                .watermarkFile(watermarkFile)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .files(new ArrayList<>())
                .build();
    }

    private Tenant buildTenantWithDocument(TenantFileStatus status) {
        Document document = buildDocument("doc-123.pdf");
        Tenant tenant = Tenant.builder()
                .id(1L)
                .status(status)
                .franceConnect(false)
                .documents(List.of(document))
                .guarantors(new ArrayList<>())
                .build();
        document.setTenant(tenant);
        return tenant;
    }

    private void setupApartmentSharing(Tenant tenant, List<ApartmentSharingLink> links) {
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setTenants(List.of(tenant));
        apartmentSharing.setApartmentSharingLinks(links);
        tenant.setApartmentSharing(apartmentSharing);
    }

    private ApartmentSharingLink buildPartnerLink(UUID token) {
        return ApartmentSharingLink.builder()
                .linkType(ApartmentSharingLinkType.PARTNER)
                .partnerId(42L)
                .fullData(true)
                .token(token)
                .build();
    }

    private ApartmentSharingLink buildFullLink(UUID token) {
        return ApartmentSharingLink.builder()
                .linkType(ApartmentSharingLinkType.LINK)
                .fullData(true)
                .token(token)
                .build();
    }

    /**
     * This method is used for all tenant account facing endpoints
     * plus the deprecated partner endpoint (/api-partner/email/{email}/tenant)
     */
    @Nested
    class ToTenantModel {

        /* When the tenant is validated and userApi is not null,
            document url must follow link path format, and token, dossierPdfUrl and dossierUrl must be set.
        */
        @Test
        void shouldUseLinkPathWhenPartnerTokenExists() {
            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.VALIDATED);
            setupApartmentSharing(tenant, List.of(buildPartnerLink(partnerToken)));

            TenantModel model = mapper.toTenantModel(tenant, userApi);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isEqualTo(partnerToken.toString());
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isEqualTo("https://api.example.com/api/application/fullPdf/" + partnerToken);
            assertThat(model.getApartmentSharing().getDossierUrl()).isEqualTo("https://example.com/file/" + partnerToken);
        }

        /* 
            When the tenant is not validated and userApi is not null,
            document url must follow link path format, but token, dossierPdfUrl and dossierUrl must be null.
        */
        @Test
        void shouldUseLinkPathWhenPartnerTokenExistsEvenIfTenantIsNotValidated() {
            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, List.of(buildPartnerLink(partnerToken)));

            TenantModel model = mapper.toTenantModel(tenant, userApi);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isNull();
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isNull();
            assertThat(model.getApartmentSharing().getDossierUrl()).isNull();
        }

        /* 
            This test is to ensure that when a partner exists (userApi is not null)  but a partner link does not exist, the path is null.
            This case should not happen, but we need to ensure that the path is null.
        */
        @Test
        void shouldUseNullPathWhenPartnerLinkDoesNotExist() {
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, new ArrayList<>());

            TenantModel model = mapper.toTenantModel(tenant, userApi);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isNull();
            assertThat(model.getApartmentSharing().getToken()).isNull();
        }

        /* When the tenant is validated and userApi is null,
            document url must follow direct path format, and token, dossierPdfUrl and dossierUrl must be set.
        */
        @Test
        void shouldUseDirectPathWhenNoUserApi() {
            UUID fullLinkToken = UUID.randomUUID();
            Tenant tenant = buildTenantWithDocument(TenantFileStatus.VALIDATED);
            setupApartmentSharing(tenant, List.of(buildFullLink(fullLinkToken)));

            TenantModel model = mapper.toTenantModel(tenant, null);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isEqualTo(fullLinkToken.toString());
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isEqualTo("https://api.example.com/api/application/fullPdf/" + fullLinkToken);
            assertThat(model.getApartmentSharing().getDossierUrl()).isEqualTo("https://example.com/file/" + fullLinkToken);
        }

        /* 
            When the tenant is not validated and userApi is null,
            document url must follow direct path format, and token, dossierPdfUrl and dossierUrl must be null.
        */
        @Test
        void shouldUseDirectPathWhenNoUserApiAndTenantIsNotValidated() {
            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, new ArrayList<>());

            TenantModel model = mapper.toTenantModel(tenant, null);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isNull();
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isNull();
            assertThat(model.getApartmentSharing().getDossierUrl()).isNull();
        }

        @Test
        void shouldUseLinkPathForGuarantorDocumentsWhenPartnerTokenExists() {
            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Document guarantorDoc = buildDocument("guarantor-doc.pdf");
            Guarantor guarantor = Guarantor.builder()
                    .id(10L)
                    .documents(List.of(guarantorDoc))
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.VALIDATED)
                    .franceConnect(false)
                    .documents(new ArrayList<>())
                    .guarantors(List.of(guarantor))
                    .build();
            guarantorDoc.setGuarantor(guarantor);
            guarantor.setTenant(tenant);
            setupApartmentSharing(tenant, List.of(buildPartnerLink(partnerToken)));

            TenantModel model = mapper.toTenantModel(tenant, userApi);

            String guarantorDocUrl = model.getGuarantors().getFirst().getDocuments().getFirst().getName();
            assertThat(guarantorDocUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/guarantor-doc.pdf");
        }

        @Test
        void shouldUseDirectPathForGuarantorDocumentsWhenNoPartnerToken() {
            Document guarantorDoc = buildDocument("guarantor-doc.pdf");
            Guarantor guarantor = Guarantor.builder()
                    .id(10L)
                    .documents(List.of(guarantorDoc))
                    .build();
            guarantorDoc.setGuarantor(guarantor);

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.VALIDATED)
                    .franceConnect(false)
                    .documents(new ArrayList<>())
                    .guarantors(List.of(guarantor))
                    .build();
            
            guarantor.setTenant(tenant);

            setupApartmentSharing(tenant, new ArrayList<>());

            TenantModel model = mapper.toTenantModel(tenant, null);

            String guarantorDocUrl = model.getGuarantors().getFirst().getDocuments().getFirst().getName();
            assertThat(guarantorDocUrl).isEqualTo("https://api.example.com/api/document/resource/guarantor-doc.pdf");
        }
    }

    @Nested
    class ToTenantModelDfc {

        /* When the tenant is validated and userApi is not null,
            document url must follow link path format, and token, dossierPdfUrl and dossierUrl must be set.
        */
        @Test
        void shouldUseLinkPathWhenPartnerTokenExists() {
            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.VALIDATED);
            setupApartmentSharing(tenant, List.of(buildPartnerLink(partnerToken)));

            ConnectedTenantModel model = mapper.toTenantModelDfc(tenant, userApi);

            String docUrl = model.getApartmentSharing().getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isEqualTo(partnerToken.toString());
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isEqualTo("https://api.example.com/api/application/fullPdf/" + partnerToken);
            assertThat(model.getApartmentSharing().getDossierUrl()).isEqualTo("https://example.com/file/" + partnerToken);
        }

        /* 
            When the tenant is not validated and userApi is not null, 
            document url must follow link path format, but token, dossierPdfUrl and dossierUrl must be null.
        */
        @Test
        void shouldUseLinkPathWhenPartnerTokenExistsEvenIfTenantIsNotValidated() {
            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, List.of(buildPartnerLink(partnerToken)));

            ConnectedTenantModel model = mapper.toTenantModelDfc(tenant, userApi);

            String docUrl = model.getApartmentSharing().getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isNull();
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isNull();
            assertThat(model.getApartmentSharing().getDossierUrl()).isNull();
        }

        /* 
            This test is to ensure that when a partner exists (userApi not null) but a partner link does not exist, the path is null.
            This case should not happen, but we need to ensure that the path is null.
        */
        @Test
        void shouldUseNullPathWhenPartnerLinkDoesNotExist() {
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, new ArrayList<>());

            ConnectedTenantModel model = mapper.toTenantModelDfc(tenant, userApi);

            String docUrl = model.getApartmentSharing().getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isNull();
            assertThat(model.getApartmentSharing().getToken()).isNull();
        }
    }

    private void setupDossierUser() {
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new TestingAuthenticationToken("user", null,
                        List.of(new SimpleGrantedAuthority("SCOPE_dossier"))));
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class FilePathRouting {

        /**
         * Users with the "dossier" OAuth scope can download individual files via /api/file/resource/{id}
         */
        @Test
        void shouldSetFilePathForDossierScopeUser() {
            setupDossierUser();

            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile()
                            .withFile(f -> f.withId(10L)))
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            assertThat(model.getDocuments().getFirst().getFiles().getFirst().getPath())
                    .isEqualTo("https://api.example.com/api/file/resource/10");
        }

        /**
         * Regular users (no "dossier" scope) must not get direct download links to individual files
         */
        @Test
        void shouldClearFilePathForNonDossierUser() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile()
                            .withFile(f -> f.withId(10L)))
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            assertThat(model.getDocuments().getFirst().getFiles().getFirst().getPath())
                    .isEmpty();
        }

        /**
         * In a COUPLE application, both partners share full access to each other's documents
         */
        @Test
        void shouldAllowFullAccessForCoupleDocs() {
            setupDossierUser();

            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("main.pdf").withWatermarkFile())
                    .inApartmentSharing(as -> as
                            .withType(ApplicationType.COUPLE)
                            .withCoTenant(ct -> ct.withId(2L)
                                    .withDocument(doc -> doc.withName("ct.pdf").withWatermarkFile()
                                            .withFile(f -> f.withId(20L)))))
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            CoTenantModel coTenant = model.getApartmentSharing().getTenants().stream()
                    .filter(t -> t.getId() == 2L).findFirst().orElseThrow();
            assertThat(coTenant.getDocuments().getFirst().getFiles().getFirst().getPath())
                    .isEqualTo("https://api.example.com/api/file/resource/20");
        }

        /**
         * In a GROUP (roommates) application, co-tenants can only see previews, not download each other's files
         */
        @Test
        void shouldPreviewOnlyForGroupDocs() {
            setupDossierUser();

            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("main.pdf").withWatermarkFile())
                    .inApartmentSharing(as -> as
                            .withType(ApplicationType.GROUP)
                            .withCoTenant(ct -> ct.withId(2L)
                                    .withDocument(doc -> doc.withName("ct.pdf").withWatermarkFile()
                                            .withFile(f -> f.withId(20L)))))
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            CoTenantModel coTenant = model.getApartmentSharing().getTenants().stream()
                    .filter(t -> t.getId() == 2L).findFirst().orElseThrow();
            assertThat(coTenant.getDocuments().getFirst().getFiles().getFirst().getPath())
                    .isEmpty();
        }

        /**
         * Guarantor documents of a co-tenant are never directly downloadable, even in a COUPLE application
         */
        @Test
        void shouldPreviewOnlyForCoTenantGuarantorDocs() {
            setupDossierUser();

            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("main.pdf").withWatermarkFile())
                    .inApartmentSharing(as -> as
                            .withType(ApplicationType.COUPLE)
                            .withCoTenant(ct -> ct.withId(2L)
                                    .withDocument(doc -> doc.withName("ct.pdf").withWatermarkFile())
                                    .withGuarantor(g -> g.withId(5L)
                                            .withDocument(doc -> doc.withName("g.pdf").withWatermarkFile()
                                                    .withFile(f -> f.withId(30L))))))
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            CoTenantModel coTenant = model.getApartmentSharing().getTenants().stream()
                    .filter(t -> t.getId() == 2L).findFirst().orElseThrow();
            assertThat(coTenant.getGuarantors().getFirst().getDocuments().getFirst().getFiles().getFirst().getPath())
                    .isEmpty();
        }
    }

    @Nested
    class CoTenantHandling {

        /**
         * The connected tenant appears both at the root model and inside apartmentSharing.tenants[].
         * To avoid data duplication, their docs/guarantors are stripped from the co-tenant list entry.
         */
        @Test
        void shouldNullifyCurrentTenantInCoTenantList() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("main.pdf").withWatermarkFile())
                    .withGuarantor(g -> g.withId(5L)
                            .withDocument(doc -> doc.withName("g.pdf").withWatermarkFile()))
                    .inApartmentSharing(as -> as
                            .withType(ApplicationType.COUPLE)
                            .withCoTenant(ct -> ct.withId(2L)
                                    .withDocument(doc -> doc.withName("ct.pdf").withWatermarkFile())))
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            CoTenantModel selfInList = model.getApartmentSharing().getTenants().stream()
                    .filter(t -> t.getId() == 1L).findFirst().orElseThrow();
            assertThat(selfInList.getDocuments()).isNull();
            assertThat(selfInList.getGuarantors()).isNull();
        }

        /**
         * Other co-tenants in the list must keep their full data (docs + guarantors) intact
         */
        @Test
        void shouldPreserveOtherCoTenantData() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("main.pdf").withWatermarkFile())
                    .inApartmentSharing(as -> as
                            .withType(ApplicationType.COUPLE)
                            .withCoTenant(ct -> ct.withId(2L)
                                    .withDocument(doc -> doc.withName("ct.pdf").withWatermarkFile())
                                    .withGuarantor(g -> g.withId(5L)
                                            .withDocument(doc -> doc.withName("g.pdf").withWatermarkFile()))))
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            CoTenantModel other = model.getApartmentSharing().getTenants().stream()
                    .filter(t -> t.getId() == 2L).findFirst().orElseThrow();
            assertThat(other.getDocuments()).isNotNull().isNotEmpty();
            assertThat(other.getGuarantors()).isNotNull().isNotEmpty();
        }
    }

    @Nested
    class AnalysisReportFiltering {

        /**
         * INFO-level analysis rules are internal and must not be exposed to tenants via the API
         */
        @Test
        void shouldFilterInfoLevelRules() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile()
                            .withAnalysisReport(r -> r.withFailedRules(
                                    rule(DocumentRuleLevel.INFO, "info-rule"),
                                    rule(DocumentRuleLevel.WARN, "warn-rule"),
                                    rule(DocumentRuleLevel.CRITICAL, "critical-rule"))))
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            var failedRules = model.getDocuments().getFirst().getDocumentAnalysisReport().getFailedRules();
            assertThat(failedRules).hasSize(2);
            assertThat(failedRules).extracting(DocumentAnalysisRule::getLevel)
                    .containsExactly(DocumentRuleLevel.WARN, DocumentRuleLevel.CRITICAL);
        }

        /**
         * WARN and CRITICAL rules are actionable by tenants and must always be visible
         */
        @Test
        void shouldKeepWarnAndCriticalRules() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile()
                            .withAnalysisReport(r -> r.withFailedRules(
                                    rule(DocumentRuleLevel.WARN, "warn-rule"),
                                    rule(DocumentRuleLevel.CRITICAL, "critical-rule"))))
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            var failedRules = model.getDocuments().getFirst().getDocumentAnalysisReport().getFailedRules();
            assertThat(failedRules).hasSize(2);
            assertThat(failedRules).extracting(DocumentAnalysisRule::getLevel)
                    .containsExactly(DocumentRuleLevel.WARN, DocumentRuleLevel.CRITICAL);
        }

        /**
         * Documents not yet analyzed must not cause NPE when the report is absent
         */
        @Test
        void shouldHandleNullAnalysisReport() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile())
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            assertThat(model.getDocuments().getFirst().getDocumentAnalysisReport()).isNull();
        }
    }

    @Nested
    class DeniedReasonsTransformation {

        /**
         * When an operator declines a document with structured reasons (messageData=true),
         * the raw parallel lists (checkedOptions + checkedOptionsId) are merged into SelectedOption objects
         */
        @Test
        void shouldBuildSelectedOptionsWithIdsWhenMessageData() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile()
                            .withDeniedReasons(d -> d.withMessageData(true)
                                    .withOptions(List.of("A", "B"), List.of(1, 2))))
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            var deniedReasons = model.getDocuments().getFirst().getDocumentDeniedReasons();
            assertThat(deniedReasons.getSelectedOptions()).hasSize(2);
            assertThat(deniedReasons.getSelectedOptions().get(0).getId()).isEqualTo(1);
            assertThat(deniedReasons.getSelectedOptions().get(0).getLabel()).isEqualTo("A");
            assertThat(deniedReasons.getSelectedOptions().get(1).getId()).isEqualTo(2);
            assertThat(deniedReasons.getSelectedOptions().get(1).getLabel()).isEqualTo("B");
            assertThat(deniedReasons.getCheckedOptions()).isNull();
            assertThat(deniedReasons.getCheckedOptionsId()).isNull();
        }

        /**
         * Legacy denial reasons (messageData=false) have labels but no IDs — options are built with null IDs
         */
        @Test
        void shouldBuildSelectedOptionsWithoutIdsWhenNotMessageData() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile()
                            .withDeniedReasons(d -> d.withMessageData(false)
                                    .withOptions(List.of("A", "B"))))
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            var deniedReasons = model.getDocuments().getFirst().getDocumentDeniedReasons();
            assertThat(deniedReasons.getSelectedOptions()).hasSize(2);
            assertThat(deniedReasons.getSelectedOptions().get(0).getId()).isNull();
            assertThat(deniedReasons.getSelectedOptions().get(0).getLabel()).isEqualTo("A");
            assertThat(deniedReasons.getSelectedOptions().get(1).getId()).isNull();
            assertThat(deniedReasons.getSelectedOptions().get(1).getLabel()).isEqualTo("B");
        }

        /**
         * Documents that were never declined must not cause NPE when denied reasons are absent
         */
        @Test
        void shouldHandleNullDeniedReasons() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withDocument(doc -> doc.withName("doc.pdf").withWatermarkFile())
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            assertThat(model.getDocuments().getFirst().getDocumentDeniedReasons()).isNull();
        }
    }

    @Nested
    class FranceConnectIdentityMapping {

        /**
         * Tenants authenticated via FranceConnect have a verified identity that is exposed separately
         * from the editable firstName/lastName fields, so the front-end can display it with a trust badge
         */
        @Test
        void shouldMapIdentityWhenFranceConnect() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .withFranceConnect("Jean", "Dupont", "Dudu")
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            assertThat(model.getFranceConnectIdentity()).isNotNull();
            assertThat(model.getFranceConnectIdentity().getFirstName()).isEqualTo("Jean");
            assertThat(model.getFranceConnectIdentity().getLastName()).isEqualTo("Dupont");
            assertThat(model.getFranceConnectIdentity().getPreferredName()).isEqualTo("Dudu");
        }

        /**
         * Tenants who did not use FranceConnect must not have a franceConnectIdentity block in the API response
         */
        @Test
        void shouldReturnNullWhenNotFranceConnect() {
            Tenant tenant = aTenant()
                    .withId(1L).withStatus(TenantFileStatus.VALIDATED)
                    .inDefaultApartmentSharing()
                    .build();

            TenantModel model = mapper.toTenantModel(tenant, null);

            assertThat(model.getFranceConnectIdentity()).isNull();
        }
    }

    @Nested
    class PreviewUrlMapping {

        @Test
        void shouldBuildPreviewUrlWithSlashBeforeFileId() {
            mapper.applicationBaseUrl = "http://test.com";

            StorageFile storageFile = new StorageFile();
            storageFile.setName("document.pdf");
            storageFile.setContentType("application/pdf");
            storageFile.setSize(42L);
            storageFile.setMd5("abc123");

            StorageFile preview = new StorageFile();
            File documentFile = File.builder()
                    .id(123L)
                    .storageFile(storageFile)
                    .preview(preview)
                    .build();

            FileModel model = mapper.toFileModel(documentFile);

            assertThat(model.getPreview()).isEqualTo("http://test.com/api/file/preview/123");
            assertThat(model.getPreview()).doesNotContain("preview123");
        }
    }
}
