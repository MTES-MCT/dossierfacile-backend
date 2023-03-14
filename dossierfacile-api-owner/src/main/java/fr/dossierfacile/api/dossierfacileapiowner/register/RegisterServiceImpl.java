package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerMapper;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerModel;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.api.dossierfacileapiowner.user.UserRoleService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Override
    public long confirmAccount(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ConfirmationTokenNotFoundException(token));
        User user = confirmationToken.getUser();
        user.setEnabled(true);
        user.setConfirmationToken(null);
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        confirmationTokenRepository.delete(confirmationToken);
        keycloakService.confirmKeycloakUser(user.getKeycloakId());
        return user.getId();
    }

    @Override
    @Transactional
    public OwnerModel register(AccountForm accountForm) {
        String email = accountForm.getEmail();
        Owner owner = ownerRepository.findByEmailAndEnabledFalse(email).orElse(new Owner("", "", email));
        // TODO : useless ?
        owner.setPassword(bCryptPasswordEncoder.encode(accountForm.getPassword()));
        owner.setKeycloakId(keycloakService.createKeycloakUserAccountCreation(accountForm, owner));
        owner.setFranceConnect(false);
        ownerRepository.save(owner);
        mailService.sendEmailConfirmAccount(owner, confirmationTokenService.createToken(owner));
        userRoleService.createRole(owner);
        return ownerMapper.toOwnerModel(owner);
    }

    @Override
    public void forgotPassword(String email) {
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (StringUtils.isBlank(owner.getKeycloakId()) || keycloakService.isKeycloakUser(owner.getKeycloakId())) {
            log.warn("User has not a valid keycloakId - ownerId" + owner.getId());
            var keycloakId = keycloakService.createKeycloakUser(email);
            owner.setKeycloakId(keycloakId);
        }
        ownerRepository.save(owner);
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(owner);
        mailService.sendEmailNewPassword(owner, passwordRecoveryToken);
    }

    @Override
    public OwnerModel createPassword(String token, String password) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordRecoveryTokenNotFoundException(token));
        User user = passwordRecoveryToken.getUser();
        user.setEnabled(true);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            var keycloakId = keycloakService.getKeycloakId(user.getEmail());
            if (keycloakId == null) {
                keycloakId = keycloakService.createKeycloakFromExistingUser(user, password);
                user.setKeycloakId(keycloakId);
            } else {
                keycloakService.createKeyCloakPassword(keycloakId, password);
                user.setKeycloakId(keycloakId);
            }
        } else {
            keycloakService.createKeyCloakPassword(user.getKeycloakId(), password);
        }
        userRepository.save(user);

        passwordRecoveryTokenRepository.delete(passwordRecoveryToken);
        return ownerMapper.toOwnerModel(ownerRepository.findById(user.getId()).orElseThrow(UserNotFoundException::new));
    }

}
