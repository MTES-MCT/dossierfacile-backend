package fr.dossierfacile.process.file.service.mfc;

import fr.dossierfacile.common.entity.File;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonFranceConnectDocument {

    private File file;
    private DocumentVerifiedContent content;

}
