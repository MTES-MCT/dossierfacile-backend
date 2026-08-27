package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerMapper;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.api.dossierfacileapiowner.user.UserRoleService;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RegisterServiceImpl Tests")
class RegisterServiceImplTest {

    private OwnerRepository ownerRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private KeycloakService keycloakService;
    private OwnerMapper ownerMapper;
    private MailService mailService;
    private ConfirmationTokenRepository confirmationTokenRepository;
    private ConfirmationTokenService confirmationTokenService;
    private PasswordRecoveryTokenService passwordRecoveryTokenService;
    private UserRepository userRepository;
    private UserRoleService userRoleService;
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private OwnerLogService ownerLogService;

    private RegisterServiceImpl registerService;

    private static final String TEST_EMAIL = "owner@example.com";
    private static final String TEST_TOKEN = "valid-token-uuid";

    @BeforeEach
    void setUp() {
        ownerRepository = mock(OwnerRepository.class);
        bCryptPasswordEncoder = mock(BCryptPasswordEncoder.class);
        keycloakService = mock(KeycloakService.class);
        ownerMapper = mock(OwnerMapper.class);
        mailService = mock(MailService.class);
        confirmationTokenRepository = mock(ConfirmationTokenRepository.class);
        confirmationTokenService = mock(ConfirmationTokenService.class);
        passwordRecoveryTokenService = mock(PasswordRecoveryTokenService.class);
        userRepository = mock(UserRepository.class);
        userRoleService = mock(UserRoleService.class);
        passwordRecoveryTokenRepository = mock(PasswordRecoveryTokenRepository.class);
        ownerLogService = mock(OwnerLogService.class);

        registerService = new RegisterServiceImpl(
                ownerRepository,
                bCryptPasswordEncoder,
                keycloakService,
                ownerMapper,
                mailService,
                confirmationTokenRepository,
                confirmationTokenService,
                passwordRecoveryTokenService,
                userRepository,
                userRoleService,
                passwordRecoveryTokenRepository,
                ownerLogService
        );
    }

    @Test
    @DisplayName("forgotPassword: Non-existing user should return silently without throwing exception")
    void testForgotPassword_nonExistingUser_shouldNotThrowException() {
        when(ownerRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> registerService.forgotPassword(TEST_EMAIL));
        verify(passwordRecoveryTokenService, never()).create(any());
        verify(mailService, never()).sendEmailNewPassword(any(), any());
    }

    @Test
    @DisplayName("forgotPassword: Existing user should create token and send email")
    void testForgotPassword_existingUser_shouldCreateTokenAndSendMail() {
        Owner owner = Owner.builder().id(1L).email(TEST_EMAIL).keycloakId("kc-id").build();
        PasswordRecoveryToken token = PasswordRecoveryToken.builder().token(TEST_TOKEN).user(owner).expirationDate(LocalDateTime.now().plusHours(2)).build();

        when(ownerRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(owner));
        when(keycloakService.isKeycloakUser("kc-id")).thenReturn(true);
        when(passwordRecoveryTokenService.create(owner)).thenReturn(token);

        registerService.forgotPassword(TEST_EMAIL);

        verify(passwordRecoveryTokenService, times(1)).create(owner);
        verify(mailService, times(1)).sendEmailNewPassword(owner, token);
    }

    @Test
    @DisplayName("createPassword: Expired token should be deleted and throw PasswordRecoveryTokenNotFoundException")
    void testCreatePassword_expiredToken_shouldThrowExceptionAndDeleteToken() {
        Owner owner = Owner.builder().id(1L).email(TEST_EMAIL).build();
        PasswordRecoveryToken expiredToken = PasswordRecoveryToken.builder()
                .token(TEST_TOKEN)
                .user(owner)
                .expirationDate(LocalDateTime.now().minusMinutes(5))
                .build();

        when(passwordRecoveryTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(expiredToken));

        assertThrows(PasswordRecoveryTokenNotFoundException.class, () -> registerService.createPassword(TEST_TOKEN, "newPassword"));
        verify(passwordRecoveryTokenRepository, times(1)).delete(expiredToken);
    }

    @Test
    @DisplayName("register: Existing unconfirmed owner should purge residual recovery token")
    void testRegister_existingUnconfirmedOwner_shouldPurgeResidualToken() {
        AccountForm form = new AccountForm();
        form.setEmail(TEST_EMAIL);
        form.setPassword("password123");

        Owner existingOwner = Owner.builder().id(1L).email(TEST_EMAIL).build();
        PasswordRecoveryToken oldToken = PasswordRecoveryToken.builder().id(10L).user(existingOwner).build();

        when(ownerRepository.findByEmailAndEnabledFalse(TEST_EMAIL)).thenReturn(Optional.of(existingOwner));
        when(passwordRecoveryTokenRepository.findByUser(existingOwner)).thenReturn(Optional.of(oldToken));
        when(keycloakService.createKeycloakUserAccountCreation(any(), any())).thenReturn("new-kc-id");

        registerService.register(form);

        verify(passwordRecoveryTokenRepository, times(1)).delete(oldToken);
    }
}
