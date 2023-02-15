package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationResult {

    private final File file;
    private final DocumentVerifiedContent content;
    private final QrCode qrCode;
    private final boolean isValid;

    public String getContentAsString() {
        return content.getElements().toString();
    }

}
