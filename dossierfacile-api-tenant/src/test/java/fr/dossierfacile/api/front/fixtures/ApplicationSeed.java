package fr.dossierfacile.api.front.fixtures;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared seed for the application full/light read path tests.
 * Must be called within an active transaction.
 *
 * Sharing 1 (COUPLE, all tenants VALIDATED — exercises the VALIDATED lastUpdateDate branch):
 * - mainTenant: ownerType SELF, franceConnect, full identity on user_account columns, trigram "DUP"
 * - coTenant: ownerType THIRD_PARTY with tenant_* columns different from user_account columns
 * - guarantors NATURAL_PERSON (mainTenant) and LEGAL_PERSON (coTenant)
 * - documents with and without watermark file, financial doc with monthlySum/customText/step
 * - one ACCOUNT_VALIDATED tenant_log row
 * - links: valid full, light (fullData=false), disabled, deleted, expired
 *
 * Sharing 2 (ALONE, INCOMPLETE): tenant with ownerType null and tenant_last_name set
 * (exercises the fallback branch of the legacy overridden getters).
 */
public final class ApplicationSeed {

    public static final String MAIN_TENANT_TRIGRAM = "DUP";

    private ApplicationSeed() {
    }

    public record Seed(
            Long sharing1Id,
            Long sharing2Id,
            Long nullStatusSharingId,
            Long mainTenantId,
            UUID validToken,
            UUID lightToken,
            UUID disabledToken,
            UUID deletedToken,
            UUID expiredToken,
            UUID otherSharingToken,
            UUID nullStatusToken,
            String tenantDocName,
            String coTenantDocName,
            String guarantorDocName,
            String watermarkedDocName
    ) {
    }

    public static Seed seed(EntityManager em) {
        // --- Sharing 1: COUPLE, all VALIDATED ---
        ApartmentSharing sharing1 = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .build();
        em.persist(sharing1);

        Tenant mainTenant = Tenant.builder()
                .email("main@test.com")
                .apartmentSharing(sharing1)
                .tenantType(TenantType.CREATE)
                .ownerType(TenantOwnerType.SELF)
                .firstName("Jean")
                .lastName("Dupont")
                .preferredName("Martin")
                .franceConnect(true)
                .zipCode("75011")
                .clarification("Dossier complet, salarié en CDI.")
                .honorDeclaration(true)
                .abroad(false)
                .status(TenantFileStatus.VALIDATED)
                .lastUpdateDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
        em.persist(mainTenant);

        Tenant coTenant = Tenant.builder()
                .email("co@test.com")
                .apartmentSharing(sharing1)
                .tenantType(TenantType.JOIN)
                .ownerType(TenantOwnerType.THIRD_PARTY)
                .firstName("AccountOwner")
                .lastName("Proxy")
                .preferredName("ProxyPref")
                .tenantFirstName("Marie")
                .tenantLastName("Durand")
                .tenantPreferredName("Bernard")
                .status(TenantFileStatus.VALIDATED)
                .lastUpdateDate(LocalDateTime.of(2025, 6, 2, 11, 0))
                .build();
        em.persist(coTenant);

        Guarantor naturalGuarantor = Guarantor.builder()
                .firstName("Guarantor")
                .lastName("One")
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(mainTenant)
                .build();
        em.persist(naturalGuarantor);

        Guarantor legalGuarantor = Guarantor.builder()
                .legalPersonName("ACME SARL")
                .typeGuarantor(TypeGuarantor.LEGAL_PERSON)
                .tenant(coTenant)
                .build();
        em.persist(legalGuarantor);

        // Documents used by the /links/{token}/documents download tests (no watermark)
        Document tenantDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .documentStatus(DocumentStatus.VALIDATED)
                .tenant(mainTenant)
                .build();
        em.persist(tenantDoc);

        Document coTenantDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .documentStatus(DocumentStatus.VALIDATED)
                .tenant(coTenant)
                .build();
        em.persist(coTenantDoc);

        Document guarantorDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .documentStatus(DocumentStatus.VALIDATED)
                .guarantor(naturalGuarantor)
                .build();
        em.persist(guarantorDoc);

        // Watermarked financial document (exercises DocumentModel.name + URL prefix in full)
        StorageFile watermark = StorageFile.builder()
                .name("watermark.pdf")
                .path("watermark/path.pdf")
                .build();
        em.persist(watermark);

        Document financialDoc = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .documentCategoryStep(DocumentCategoryStep.TENANT_PROOF)
                .documentStatus(DocumentStatus.VALIDATED)
                .monthlySum(2500)
                .customText("Prime annuelle incluse.")
                .watermarkFile(watermark)
                .tenant(mainTenant)
                .build();
        em.persist(financialDoc);

        Document legalGuarantorDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION_LEGAL_PERSON)
                .documentSubCategory(DocumentSubCategory.LEGAL_PERSON)
                .documentStatus(DocumentStatus.VALIDATED)
                .guarantor(legalGuarantor)
                .build();
        em.persist(legalGuarantorDoc);

        // ACCOUNT_VALIDATED log (exercises the VALIDATED lastUpdateDate branch)
        TenantLog validationLog = new TenantLog(LogType.ACCOUNT_VALIDATED, mainTenant.getId());
        em.persist(validationLog);

        // Links for sharing 1
        UUID validToken = UUID.randomUUID();
        em.persist(link(sharing1, validToken, true, false, false, null));

        UUID lightToken = UUID.randomUUID();
        em.persist(link(sharing1, lightToken, false, false, false, null));

        UUID disabledToken = UUID.randomUUID();
        em.persist(link(sharing1, disabledToken, true, true, false, null));

        UUID deletedToken = UUID.randomUUID();
        em.persist(link(sharing1, deletedToken, true, false, true, null));

        UUID expiredToken = UUID.randomUUID();
        em.persist(link(sharing1, expiredToken, true, false, false, LocalDateTime.of(2020, 1, 1, 0, 0)));

        // --- Sharing 2: ALONE, INCOMPLETE, ownerType null with tenant_last_name fallback ---
        ApartmentSharing sharing2 = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build();
        em.persist(sharing2);

        Tenant otherTenant = Tenant.builder()
                .email("other@test.com")
                .apartmentSharing(sharing2)
                .tenantType(TenantType.CREATE)
                .tenantFirstName("Paul")
                .tenantLastName("Fallback")
                .status(TenantFileStatus.INCOMPLETE)
                .build();
        em.persist(otherTenant);

        Document otherDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(otherTenant)
                .build();
        em.persist(otherDoc);

        UUID otherSharingToken = UUID.randomUUID();
        em.persist(link(sharing2, otherSharingToken, true, false, false, null));

        // --- Sharing 3: ALONE with a NULL status tenant (2317 such rows in prod) ---
        // The legacy getter computes the status on the fly; the new read path must do the same.
        ApartmentSharing nullStatusSharing = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build();
        em.persist(nullStatusSharing);

        Tenant nullStatusTenant = Tenant.builder()
                .email("nostatus@test.com")
                .apartmentSharing(nullStatusSharing)
                .tenantType(TenantType.CREATE)
                .tenantLastName("Nostat")
                .status(null)
                .build();
        em.persist(nullStatusTenant);

        Document nullStatusTenantDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .documentStatus(DocumentStatus.TO_PROCESS)
                .tenant(nullStatusTenant)
                .build();
        em.persist(nullStatusTenantDoc);

        UUID nullStatusToken = UUID.randomUUID();
        em.persist(link(nullStatusSharing, nullStatusToken, true, false, false, null));

        em.flush();

        return new Seed(
                sharing1.getId(),
                sharing2.getId(),
                nullStatusSharing.getId(),
                mainTenant.getId(),
                validToken,
                lightToken,
                disabledToken,
                deletedToken,
                expiredToken,
                otherSharingToken,
                nullStatusToken,
                tenantDoc.getName(),
                coTenantDoc.getName(),
                guarantorDoc.getName(),
                financialDoc.getName()
        );
    }

    private static ApartmentSharingLink link(ApartmentSharing sharing, UUID token, boolean fullData,
                                             boolean disabled, boolean deleted, LocalDateTime expirationDate) {
        return ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .token(token)
                .fullData(fullData)
                .disabled(disabled)
                .deleted(deleted)
                .linkType(ApartmentSharingLinkType.LINK)
                .expirationDate(expirationDate)
                .build();
    }
}
