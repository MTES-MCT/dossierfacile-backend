package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRuleLevel;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

    @Nested
    class ToTenantModel {

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

        @Test
        void shouldUseDirectPathWhenNotValidated() {
            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).name("partner-test").build();

            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, List.of(buildPartnerLink(partnerToken)));

            TenantModel model = mapper.toTenantModel(tenant, userApi);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isNull();
            assertThat(model.getApartmentSharing().getDossierPdfUrl()).isNull();
        }

        @Test
        void shouldUseDirectPathWhenNoUserApi() {
            Tenant tenant = buildTenantWithDocument(TenantFileStatus.VALIDATED);
            setupApartmentSharing(tenant, new ArrayList<>());

            TenantModel model = mapper.toTenantModel(tenant, null);

            String docUrl = model.getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
        }

        @Test
        void shouldRewriteGuarantorDocumentUrls() {
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
    }

    @Nested
    class ToTenantModelDfc {

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
        }

        @Test
        void shouldUseDirectPathWhenNoPartnerToken() {
            Tenant tenant = buildTenantWithDocument(TenantFileStatus.INCOMPLETE);
            setupApartmentSharing(tenant, new ArrayList<>());

            ConnectedTenantModel model = mapper.toTenantModelDfc(tenant, null);

            String docUrl = model.getApartmentSharing().getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
            assertThat(model.getApartmentSharing().getToken()).isNull();
        }
    }
}
