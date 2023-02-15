package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@Getter
@AllArgsConstructor
public class ValidationResult {

    private final File file;
    private final DocumentVerifiedContent content;
    private final QrCode qrCode;
    private final ValidationStatus validationStatus;

    static ValidationResult error(File file, QrCode qrCode) {
        return new ValidationResult(file, null, qrCode, ValidationStatus.API_ERROR);
    }

    public String getContentAsString() {
        return content.getElements().toString();
    }

    public boolean isValid() {
        return validationStatus == ValidationStatus.VALID;
    }

}
