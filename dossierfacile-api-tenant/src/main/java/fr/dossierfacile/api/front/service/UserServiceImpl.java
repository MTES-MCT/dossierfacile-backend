package fr.dossierfacile.api.front.service;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.form.DeleteAccountForm;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.repository.AccountDeleteLogRepository;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.AccountDeleteLog;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final OvhService ovhService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final AccountDeleteLogRepository accountDeleteLogRepository;
    private final TenantMapper tenantMapper;
    private final TenantRepository tenantRepository;
    private final Gson gson = new Gson();

    @Override
    public void confirmAccount(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ConfirmationTokenNotFoundException(token));
        User user = confirmationToken.getUser();
        user.setEnabled(true);
        user.setConfirmationToken(null);
        userRepository.save(user);
        confirmationTokenRepository.delete(confirmationToken);
    }

    @Override
    public TenantModel createPassword(String token, String password) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordRecoveryTokenNotFoundException(token));
        User user = passwordRecoveryToken.getUser();
        user.setEnabled(true);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(user);
        passwordRecoveryTokenRepository.delete(passwordRecoveryToken);
        return tenantMapper.toTenantModel(tenantRepository.getOne(user.getId()));
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(user);
        mailService.sendEmailNewPassword(user, passwordRecoveryToken);
    }

    @Override
    public Boolean deleteAccount(Tenant tenant, DeleteAccountForm deleteAccountForm) {
        if (bCryptPasswordEncoder.matches(deleteAccountForm.getPassword(), tenant.getPassword())) {
            mailService.sendEmailAccountDeleted(tenant);
            this.savingJsonProfileBeforeDeletion(tenantMapper.toTenantModel(tenant));
            Optional.ofNullable(tenant.getDocuments())
                    .orElse(new ArrayList<>())
                    .forEach(this::deleteFilesFromStorage);
            Optional.ofNullable(tenant.getGuarantors())
                    .orElse(new ArrayList<>())
                    .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                            .orElse(new ArrayList<>())
                            .forEach(this::deleteFilesFromStorage)
                    );

            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            if (tenant.getTenantType() == TenantType.CREATE || apartmentSharing.getNumberOfTenants() == 1) {
                log.info("Removing apartment_sharing with id [" + apartmentSharing.getId() + "] with [" + apartmentSharing.getNumberOfTenants() + "] tenants");
                apartmentSharingRepository.delete(apartmentSharing);
            } else {
                log.info("Removing user/tenant with id [" + tenant.getId() + "]");
                userRepository.delete(tenant);
            }
            return true;
        }
        return false;
    }

    private void savingJsonProfileBeforeDeletion(TenantModel tenantModel) {
        accountDeleteLogRepository.save(
                AccountDeleteLog.builder()
                        .userId(tenantModel.getId())
                        .deletionDate(LocalDateTime.now())
                        .jsonProfileBeforeDeletion(gson.toJson(tenantModel))
                        .build()
        );
    }

    private void deleteFilesFromStorage(Document document) {
        List<File> files = document.getFiles();
        if (files != null && !files.isEmpty()) {
            log.info("Removing files from storage of document with id [" + document.getId() + "]");
            ovhService.delete(files.stream().map(File::getPath).collect(Collectors.toList()));
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            ovhService.delete(document.getName());
        }
    }
}
