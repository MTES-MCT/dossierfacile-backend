package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryTokenServiceImpl implements PasswordRecoveryTokenService {
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Override
    public PasswordRecoveryToken create(User user) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByUser(user).orElse(
                PasswordRecoveryToken.builder().token(UUID.randomUUID().toString()).user(user).build());
        return passwordRecoveryTokenRepository.save(passwordRecoveryToken);
    }
}
