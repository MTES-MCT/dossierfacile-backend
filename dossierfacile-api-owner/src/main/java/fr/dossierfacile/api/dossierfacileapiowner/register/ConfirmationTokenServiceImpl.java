package fr.dossierfacile.api.dossierfacileapiowner.register;

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
