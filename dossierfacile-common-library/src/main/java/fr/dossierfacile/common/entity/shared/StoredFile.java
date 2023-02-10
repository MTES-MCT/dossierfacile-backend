package fr.dossierfacile.common.entity.shared;

import fr.dossierfacile.common.entity.EncryptionKey;

public interface StoredFile {
    String getPath();

    EncryptionKey getEncryptionKey();

}
