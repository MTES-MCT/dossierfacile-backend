package fr.dossierfacile.api.pdfgenerator.util.parameterresolvers;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TenantResolver extends TypeBasedParameterResolver<Tenant> {

    @Override
    public Tenant resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return buildTenant();
    }

    static Tenant buildTenant() {
        Tenant tenant = Tenant.builder().build();
        tenant.setId(1L);
        tenant.setFirstName("Dr");
        tenant.setLastName("Who");
        tenant.setGuarantors(new ArrayList<>());
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenant.setHonorDeclaration(true);
        tenant.setTenantType(TenantType.CREATE);
        tenant.setEmail("dr@tardis.fr");
        tenant.setClarification("""
                Bonjour test truc anrs anruiste anruise tanruiset arnusiet arnusi etaurnsiet arnuiset
                anruise tanruiset anruise tanrusiet anruiset anrusiet anruset nrauist rste aurnsiet anrusiet anrsa ute narusix rauia anuxrisa aurnanruise tanruiset anruise tanrusiet anruiset anrusiet anruset nrauist rste aurnsiet anrusiet anrsa ute narusix rauia anuxrisa aurn

                test smiley \uD83D\uDE08 auie
                test smiley \uD83D\uDE08 xi
                """);

        List<Document> documents = buildDocuments(tenant);

        tenant.setDocuments(documents);

        return tenant;
    }

    private static StorageFile buildStorageFile() {
        return StorageFile.builder()
                .path("CNI.pdf")
                .provider(ObjectStorageProvider.LOCAL)
                .providers(Collections.singletonList(ObjectStorageProvider.LOCAL.name()))
                .build();
    }

    private static List<Document> buildDocuments(Tenant tenant) {
        Document professional = new Document();
        professional.setDocumentCategory(DocumentCategory.PROFESSIONAL);
        professional.setDocumentSubCategory(DocumentSubCategory.CDI);
        professional.setDocumentStatus(DocumentStatus.VALIDATED);
        professional.setTenant(tenant);
        professional.setWatermarkFile(buildStorageFile());

        Document financial = new Document();
        financial.setDocumentCategory(DocumentCategory.FINANCIAL);
        financial.setDocumentSubCategory(DocumentSubCategory.SALARY);
        financial.setDocumentStatus(DocumentStatus.VALIDATED);
        financial.setTenant(tenant);
        financial.setMonthlySum(3000);
        financial.setWatermarkFile(buildStorageFile());

        Document tax = new Document();
        tax.setDocumentCategory(DocumentCategory.TAX);
        tax.setDocumentSubCategory(DocumentSubCategory.LESS_THAN_YEAR);
        tax.setDocumentStatus(DocumentStatus.VALIDATED);
        tax.setTenant(tenant);
        tax.setWatermarkFile(buildStorageFile());

        Document identification = new Document();
        identification.setDocumentCategory(DocumentCategory.IDENTIFICATION);
        identification.setDocumentSubCategory(DocumentSubCategory.FRENCH_PASSPORT);
        identification.setDocumentStatus(DocumentStatus.VALIDATED);
        identification.setTenant(tenant);
        identification.setWatermarkFile(buildStorageFile());

        Document residency = new Document();
        residency.setDocumentCategory(DocumentCategory.RESIDENCY);
        residency.setDocumentSubCategory(DocumentSubCategory.TENANT);
        residency.setDocumentStatus(DocumentStatus.VALIDATED);
        residency.setTenant(tenant);
        residency.setWatermarkFile(buildStorageFile());

        return List.of(professional, financial, tax, identification, residency);
    }

}
