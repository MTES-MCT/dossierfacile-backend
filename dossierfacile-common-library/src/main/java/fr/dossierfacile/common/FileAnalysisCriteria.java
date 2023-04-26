package fr.dossierfacile.common;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;

import java.util.Arrays;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SCHOLARSHIP;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SOCIAL_SERVICE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.STUDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.UNEMPLOYED;

public class FileAnalysisCriteria {

    private static final List<TenantFileStatus> IGNORED_STATUS = List.of(TenantFileStatus.VALIDATED, TenantFileStatus.ARCHIVED);

    public static boolean shouldBeAnalyzed(File file) {
        Document document = file.getDocument();
        Tenant tenant = document.getTenant();
        if (IGNORED_STATUS.contains(tenant.getStatus())) {
            return false;
        }
        return switch (document.getDocumentCategory()) {
            case TAX -> hasSubCategory(document, MY_NAME);
            case PROFESSIONAL -> hasSubCategory(document, STUDENT, UNEMPLOYED);
            case FINANCIAL -> hasSubCategory(document, SALARY, SCHOLARSHIP, SOCIAL_SERVICE);
            case IDENTIFICATION, RESIDENCY, IDENTIFICATION_LEGAL_PERSON, NULL -> false;
        };
    }

    private static boolean hasSubCategory(Document document, DocumentSubCategory... subCategories) {
        return Arrays.asList(subCategories).contains(document.getDocumentSubCategory());
    }

}
