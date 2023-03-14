package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.MonFranceConnectValidationResult;
import fr.dossierfacile.common.enums.MonFranceConnectValidationStatus;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.API_ERROR;
import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.WRONG_CATEGORY;

@Getter
@ToString
@AllArgsConstructor
public class ValidationResult {

    private final DocumentVerifiedContent content;
    private final MonFranceConnectValidationStatus validationStatus;

    static ValidationResult error() {
        return new ValidationResult( null, API_ERROR);
    }

    public static ValidationResult wrongCategory() {
        return new ValidationResult(null, WRONG_CATEGORY);
    }

    public MonFranceConnectValidationResult toEntity(File file, QrCode qrCode) {
        MonFranceConnectValidationResult entity = new MonFranceConnectValidationResult();
        entity.setFile(file);
        entity.setQrCodeContent(qrCode.getContent());
        entity.setApiResponse(Optional.ofNullable(content)
                .map(DocumentVerifiedContent::getElements)
                .orElse(null));
        entity.setValidationStatus(validationStatus);
        return entity;
    }

}
