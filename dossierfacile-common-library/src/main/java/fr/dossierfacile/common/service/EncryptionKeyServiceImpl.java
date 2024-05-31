package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.EncryptionKeyStatus;
import fr.dossierfacile.common.repository.EncryptionKeyRepository;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
@AllArgsConstructor
public class EncryptionKeyServiceImpl implements EncryptionKeyService {
    private final EncryptionKeyRepository repository;

    @Override
    public EncryptionKey getCurrentKey() {
        return repository.findByStatus(EncryptionKeyStatus.CURRENT).orElseGet(() -> {
            try {
                KeyGenerator keygen = KeyGenerator.getInstance("AES");
                SecretKey key = keygen.generateKey();

                return repository.save(
                        EncryptionKey.builder()
                                .algorithm(key.getAlgorithm())
                                .format(key.getFormat())
                                .encodedSecret(key.getEncoded())
                                .status(EncryptionKeyStatus.CURRENT)
                                .version(2)
                                .build());

            } catch (NoSuchAlgorithmException e) {
                log.error("Unable to generate secret key for file encryption", e);
            }
            return null;
        });
    }

}