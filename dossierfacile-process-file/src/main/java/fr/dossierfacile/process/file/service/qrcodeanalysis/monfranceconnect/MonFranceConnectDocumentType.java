package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.qrcodeanalysis.GuessedDocumentCategory;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.PROFESSIONAL;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SOCIAL_SERVICE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.STUDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.UNEMPLOYED;

@AllArgsConstructor
public enum MonFranceConnectDocumentType {

    TAXABLE_INCOME("Justificatif de revenus imposables"),
    SCHOLARSHIP("Justificatif de statut étudiant boursier"),
    STUDENT_STATUS("Justificatif de statut étudiant"),
    UNEMPLOYMENT_STATUS("Justificatif d'inscription à Pôle emploi"),
    UNEMPLOYMENT_BENEFIT("Justificatif d'indemnisation à Pôle emploi"),
    UNKNOWN("");

    private final String documentTitle;

    public static MonFranceConnectDocumentType of(InMemoryPdfFile file) {
        String contentAsString = file.getContentAsString();
        return Arrays.stream(MonFranceConnectDocumentType.values())
                .filter(type -> !type.equals(UNKNOWN))
                .filter(type -> contentAsString.contains(type.documentTitle))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public Optional<GuessedDocumentCategory> getCategory() {
        GuessedDocumentCategory guess = switch (this) {
            case TAXABLE_INCOME -> new GuessedDocumentCategory(TAX, MY_NAME);
            case SCHOLARSHIP -> new GuessedDocumentCategory(FINANCIAL, DocumentSubCategory.SCHOLARSHIP);
            case STUDENT_STATUS -> new GuessedDocumentCategory(PROFESSIONAL, STUDENT);
            case UNEMPLOYMENT_STATUS -> new GuessedDocumentCategory(PROFESSIONAL, UNEMPLOYED);
            case UNEMPLOYMENT_BENEFIT -> new GuessedDocumentCategory(FINANCIAL, SOCIAL_SERVICE);
            case UNKNOWN -> null;
        };
        return Optional.ofNullable(guess);
    }

}
