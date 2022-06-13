package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryTokenServiceImpl implements PasswordRecoveryTokenService {
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Override
    public PasswordRecoveryToken create(Owner owner) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByUser(owner).orElse(
                PasswordRecoveryToken.builder().token(UUID.randomUUID().toString()).user(owner).build());
        return passwordRecoveryTokenRepository.save(passwordRecoveryToken);
    }
}
