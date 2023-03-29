package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConfirmationTokenServiceImpl implements ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;

    @Override
    public ConfirmationToken createToken(User user) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByUser(user).orElse(new ConfirmationToken(user));
        confirmationToken.refreshToken();
        return confirmationTokenRepository.save(confirmationToken);
    }
}
