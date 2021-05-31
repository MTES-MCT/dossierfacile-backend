package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.User;
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
