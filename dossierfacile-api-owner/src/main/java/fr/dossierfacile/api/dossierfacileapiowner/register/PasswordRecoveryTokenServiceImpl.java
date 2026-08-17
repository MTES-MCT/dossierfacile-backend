package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryTokenServiceImpl implements PasswordRecoveryTokenService {
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Override
    public PasswordRecoveryToken create(Owner owner) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByUser(owner)
                .orElseGet(() -> PasswordRecoveryToken.builder().user(owner).build());
        passwordRecoveryToken.setToken(UUID.randomUUID().toString());
        passwordRecoveryToken.setExpirationDate(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2));
        return passwordRecoveryTokenRepository.save(passwordRecoveryToken);
    }
}
