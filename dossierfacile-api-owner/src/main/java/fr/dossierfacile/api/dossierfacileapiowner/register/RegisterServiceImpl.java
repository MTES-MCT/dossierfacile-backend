package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerMapper;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerModel;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.api.dossierfacileapiowner.user.UserRoleService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class RegisterServiceImpl implements RegisterService {
    private final OwnerRepository ownerRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final KeycloakService keycloakService;
    private final OwnerMapper ownerMapper;
    private final MailService mailService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    @Override
    public long confirmAccount(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ConfirmationTokenNotFoundException(token));
        User user = confirmationToken.getUser();
        user.setEnabled(true);
        user.setConfirmationToken(null);
        userRepository.save(user);
        confirmationTokenRepository.delete(confirmationToken);
        keycloakService.confirmKeycloakUser(user.getKeycloakId());
        return user.getId();
    }

    @Override
    @Transactional
    public OwnerModel register(AccountForm accountForm) {
            String email = accountForm.getEmail().toLowerCase();
            Owner owner = ownerRepository.findByEmailAndEnabledFalse(email).orElse(new Owner("", "", email));
            owner.setPassword(bCryptPasswordEncoder.encode(accountForm.getPassword()));
            owner.setKeycloakId(keycloakService.createKeycloakUserAccountCreation(accountForm, owner));
            ownerRepository.save(owner);
            mailService.sendEmailConfirmAccount(owner, confirmationTokenService.createToken(owner));
            userRoleService.createRole(owner);
            return ownerMapper.toOwnerModel(owner);
    }
}
