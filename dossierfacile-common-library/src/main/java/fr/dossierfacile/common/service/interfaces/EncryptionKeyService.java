package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.EncryptionKey;

public interface EncryptionKeyService {
    /**
     * Gets the current SecretKey to encrypt file
     */
    EncryptionKey getCurrentKey();
}