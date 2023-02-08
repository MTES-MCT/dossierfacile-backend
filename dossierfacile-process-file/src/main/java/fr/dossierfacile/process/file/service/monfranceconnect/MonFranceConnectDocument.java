package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonFranceConnectDocument {

    private final File file;
    private final DocumentVerifiedContent content;
    private final boolean isValid;

    public String getContentAsString() {
        return content.getElements().toString();
    }

}
