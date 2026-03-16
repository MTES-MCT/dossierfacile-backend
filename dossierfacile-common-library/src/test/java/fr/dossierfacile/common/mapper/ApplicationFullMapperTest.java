package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFullMapperTest {

    @Nested
    class ToApplicationModelWithToken {

        @Test
        void shouldRewriteDocumentUrlsToLinkPath() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(apartmentSharing);

            UUID token = UUID.randomUUID();

            ApplicationModel model = mapper.toApplicationModelWithToken(apartmentSharing, token);

            assertThat(model.getTenants()).hasSize(1);
            assertThat(model.getTenants().getFirst().getDocuments()).hasSize(1);

            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + token + "/documents/doc-123.pdf");
            assertThat(model.getDossierPdfUrl()).isEqualTo("https://api.example.com/api/application/fullPdf/" + token);
            assertThat(model.getDossierUrl()).isEqualTo("https://example.com/file/" + token);
        }

        @Test
        void shouldSetNullUrlWhenNoWatermarkFile() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(null)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(apartmentSharing);

            UUID token = UUID.randomUUID();

            ApplicationModel model = mapper.toApplicationModelWithToken(apartmentSharing, token);

            assertThat(model.getTenants().getFirst().getDocuments().getFirst().getName()).isNull();
        }
    }

    @Nested
    class ToApplicationModelWithUserApi {

        /* When the tenant is validated and userApi is not null,
            document url must follow link path format, and token, dossierPdfUrl and dossierUrl must be set.
        */
        @Test
        void shouldUseLinkPathWhenPartnerTokenExists() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.VALIDATED)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).build();

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(List.of(
                    ApartmentSharingLink.builder()
                            .linkType(ApartmentSharingLinkType.PARTNER)
                            .partnerId(42L)
                            .fullData(true)
                            .token(partnerToken)
                            .build()
            ));
            tenant.setApartmentSharing(apartmentSharing);

            ApplicationModel model = mapper.toApplicationModel(apartmentSharing, userApi);

            assertThat(model.getTenants()).hasSize(1);
            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/doc-123.pdf");
            assertThat(model.getDossierPdfUrl()).isEqualTo("https://api.example.com/api/application/fullPdf/" + partnerToken);
            assertThat(model.getDossierUrl()).isEqualTo("https://example.com/file/" + partnerToken);
        }

        /* 
            When the tenant is not validated and userApi is not null,
            document url must follow link path format, but token, dossierPdfUrl and dossierUrl must be null.
        */
        @Test
        void shouldUseLinkPathWhenPartnerTokenExistsEvenIfTenantIsNotValidated() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.INCOMPLETE)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            UUID partnerToken = UUID.randomUUID();
            UserApi userApi = UserApi.builder().id(42L).build();

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(List.of(
                    ApartmentSharingLink.builder()
                            .linkType(ApartmentSharingLinkType.PARTNER)
                            .partnerId(42L)
                            .fullData(true)
                            .token(partnerToken)
                            .build()
            ));
            tenant.setApartmentSharing(apartmentSharing);

            ApplicationModel model = mapper.toApplicationModel(apartmentSharing, userApi);

            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/application/links/" + partnerToken + "/documents/doc-123.pdf");
            assertThat(model.getDossierPdfUrl()).isNull();
            assertThat(model.getDossierUrl()).isNull();
        }

        /* 
            When the tenant is not validated and userApi is null,
            document url must follow direct path format, but token, dossierPdfUrl and dossierUrl must be null.
        */
        @Test
        void shouldUseResourcePathWhenPartneIsNullAndTenantIsNotValidated() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            // Tenant with INCOMPLETE status → ApartmentSharing.getStatus() returns INCOMPLETE
            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.INCOMPLETE)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(apartmentSharing);

            ApplicationModel model = mapper.toApplicationModel(apartmentSharing, null);

            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
            assertThat(model.getDossierPdfUrl()).isNull();
        }

        @Test
        void shouldUseNullPathWhenPartnerLinkDoesNotExist() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.VALIDATED)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(apartmentSharing);

            UserApi userApi = UserApi.builder().id(42L).build();

            ApplicationModel model = mapper.toApplicationModel(apartmentSharing, userApi);

            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isNull();
            assertThat(model.getDossierPdfUrl()).isNull();
        }

        @Test
        void shouldUseResourcePathWhenNoUserApi() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .status(TenantFileStatus.VALIDATED)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(apartmentSharing);

            ApplicationModel model = mapper.toApplicationModel(apartmentSharing, null);

            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
            assertThat(model.getDossierPdfUrl()).isNull();
        }
    }

    @Nested
    class ToApplicationModelWithoutToken {

        @Test
        void shouldUseResourcePath() {
            ApplicationFullMapperImpl mapper = new ApplicationFullMapperImpl();
            mapper.applicationBaseUrl = "https://api.example.com";
            mapper.tenantBaseUrl = "https://example.com";

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .name("doc-123.pdf")
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .files(new ArrayList<>())
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .documents(List.of(document))
                    .guarantors(new ArrayList<>())
                    .build();
            document.setTenant(tenant);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setTenants(List.of(tenant));
            apartmentSharing.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(apartmentSharing);

            ApplicationModel model = mapper.toApplicationModel(apartmentSharing);

            assertThat(model.getTenants()).hasSize(1);
            String docUrl = model.getTenants().getFirst().getDocuments().getFirst().getName();
            assertThat(docUrl).isEqualTo("https://api.example.com/api/document/resource/doc-123.pdf");
        }
    }
}