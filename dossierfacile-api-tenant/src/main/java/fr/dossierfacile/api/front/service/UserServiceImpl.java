package fr.dossierfacile.api.front.service;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.form.PartnerForm;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.api.front.register.form.tenant.FranceConnectTaxForm;
import fr.dossierfacile.api.front.repository.AccountDeleteLogRepository;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.AccountDeleteLog;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TaxFileExtractionType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.type.TaxDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final FileStorageService fileStorageService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final AccountDeleteLogRepository accountDeleteLogRepository;
    private final TenantMapper tenantMapper;
    private final TenantCommonRepository tenantRepository;
    private final LogService logService;
    private final Gson gson = new Gson();
    private final KeycloakService keycloakService;
    private final SourceService sourceService;
    private final PartnerCallBackService partnerCallBackService;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentRepository documentRepository;

    @Value("${dgfip.token}")
    private String dgfipToken;
    @Value("${dgfip.api.url}")
    private String dgfipApiUrl;

    @Value("${dgfip.id.teleservice}")
    private String idTeleservice;

    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public long confirmAccount(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ConfirmationTokenNotFoundException(token));
        User user = confirmationToken.getUser();
        user.setEnabled(true);
        user.setConfirmationToken(null);
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        confirmationTokenRepository.delete(confirmationToken);
        keycloakService.confirmKeycloakUser(user.getKeycloakId());
        tenantRepository.resetWarnings(user.getId());
        return user.getId();
    }

    @Override
    public TenantModel createPassword(User user, String password) {
        user.setEnabled(true);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        if (user.getKeycloakId() == null) {
            var keycloakId = keycloakService.getKeycloakId(user.getEmail());
            keycloakService.createKeyCloakPassword(keycloakId, password);
            user.setKeycloakId(keycloakId);
        } else {
            keycloakService.createKeyCloakPassword(user.getKeycloakId(), password);
        }
        userRepository.save(user);

        return tenantMapper.toTenantModel(tenantRepository.getOne(user.getId()));
    }

    @Override
    public TenantModel createPassword(String token, String password) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordRecoveryTokenNotFoundException(token));

        TenantModel tenantModel = createPassword(passwordRecoveryToken.getUser(), password);

        passwordRecoveryTokenRepository.delete(passwordRecoveryToken);
        return tenantModel;
    }

    @Override
    public void forgotPassword(String email) {
        Tenant tenant = tenantRepository.findOneByEmail(email);
        if (tenant == null) {
            throw new UserNotFoundException(email);
        }
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(tenant);
        mailService.sendEmailNewPassword(tenant, passwordRecoveryToken);
    }

    @Override
    public void deleteAccount(Tenant tenant) {
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DELETED_ACCOUNT);
        saveAndDeleteInfoByTenant(tenant);
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (tenant.getTenantType() == TenantType.CREATE || apartmentSharing.getNumberOfTenants() == 1) {
            log.info("Removing apartment_sharing with id [" + apartmentSharing.getId() + "] with [" + apartmentSharing.getNumberOfTenants() + "] tenants");
            keycloakService.deleteKeycloakUsers(apartmentSharing.getTenants());
            apartmentSharingRepository.delete(apartmentSharing);
        } else {
            log.info("Removing user/tenant with id [" + tenant.getId() + "]");
            logService.saveLog(LogType.ACCOUNT_DELETE, tenant.getId());
            keycloakService.deleteKeycloakUser(tenant);
            userRepository.delete(tenant);
        }
    }

    @Override
    public Boolean deleteCoTenant(Tenant tenant, Long id) {
        if (tenant.getTenantType().equals(TenantType.CREATE)) {
            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            Tenant coTenant = apartmentSharing.getTenants().stream().filter(t -> t.getId().equals(id) && t.getTenantType().equals(TenantType.JOIN)).findFirst().orElseThrow(null);
            if (coTenant != null) {
                partnerCallBackService.sendCallBack(coTenant, PartnerCallBackType.DELETED_ACCOUNT);
                if (coTenant.getKeycloakId() != null) {
                    keycloakService.deleteKeycloakUser(coTenant);
                }
                saveAndDeleteInfoByTenant(coTenant);
                userRepository.delete(coTenant);
                apartmentSharing.getTenants().remove(coTenant);
                updateApplicationTypeOfApartmentAfterDeletionOfCotenant(apartmentSharing);
                apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);
                logService.saveLog(LogType.ACCOUNT_DELETE, id);
                return true;
            }
        }
        return false;
    }

    @Override
    public EmailExistsModel emailExists(EmailExistsForm emailExistsForm) {
        return EmailExistsModel.builder()
                .email(emailExistsForm.getEmail())
                .exists(userRepository.existsByEmail(emailExistsForm.getEmail()))
                .build();
    }

    @Override
    public void linkTenantToPartner(Tenant tenant, PartnerForm partnerForm) {
        if (partnerForm.getSource() != null) {
            UserApi userApi = sourceService.findOrCreate(partnerForm.getSource());
            partnerCallBackService.registerTenant(partnerForm.getInternalPartnerId(), tenant, userApi);
        }
    }

    @Override
    public void linkTenantToPartner(Tenant tenant, String partner) {
        sourceService.findByName(partner).ifPresent(userApi -> partnerCallBackService.registerTenant(null, tenant, userApi));
    }

    @Override
    public void linkTenantToApiPartner(Tenant tenant, String partner) {
        if (partner != null) {
            UserApi userApi = sourceService.findOrCreate(partner);
            partnerCallBackService.linkTenantToPartner(null, tenant, userApi);
        }
    }

    @Override
    public void logout(Tenant tenant) {
        keycloakService.logout(tenant);
    }

    @Override
    public void unlinkFranceConnect(Tenant tenant) {
        User user = userRepository.findById(tenant.getId()).orElseThrow(IllegalArgumentException::new);
        user.setFranceConnect(false);
        userRepository.save(tenant);
        keycloakService.unlinkFranceConnect(tenant);
    }

    private void saveAndDeleteInfoByTenant(Tenant tenant) {
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
            fileStorageService.delete(files.stream().map(File::getPath).collect(Collectors.toList()));
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            fileStorageService.delete(document.getName());
        }
    }

    private void updateApplicationTypeOfApartmentAfterDeletionOfCotenant(ApartmentSharing apartmentSharing) {
        ApplicationType nextApplicationType = ApplicationType.ALONE;

        //Current application can only be in this point [COUPLE] or [GROUP]
        ApplicationType previousApplicationType = apartmentSharing.getApplicationType();

        //If previous application was a [GROUP] and after deletion of 1 cotenant, it has now (>=2) tenants then,
        // it will stay as an application [GROUP]. Otherwise it will become an application [ALONE]
        if (previousApplicationType == ApplicationType.GROUP
                && apartmentSharing.getNumberOfTenants() >= 2) {
            nextApplicationType = ApplicationType.GROUP;
        }

        if (previousApplicationType != nextApplicationType) {
            log.info("Changing applicationType of apartment with ID [" + apartmentSharing.getId() + "] from [" + previousApplicationType.name() + "] to [" + nextApplicationType.name() + "]");
            apartmentSharing.setApplicationType(nextApplicationType);
            apartmentSharingRepository.save(apartmentSharing);
        }
    }

    @Override
    @Transactional
    public void checkDGFIPApi(Tenant tenant, FranceConnectTaxForm franceConnectTaxForm) {
        String fcToken = franceConnectTaxForm.getFcToken();
        String dgfipBearerToken = getDGFIPBearerToken();
        TaxDocument taxDocument = getLatestTaxFromDGFIPByFC(dgfipBearerToken, fcToken);
        if (taxDocument == null) {
            return;
        }
        Document currentTaxDocument = tenant.getDocuments().stream().filter(document -> document.getDocumentCategory() == DocumentCategory.TAX).findFirst().orElse(Document.builder()
                .documentCategory(DocumentCategory.TAX)
                .tenant(tenant)
                .build());
        if (currentTaxDocument.getId() == null) {
            currentTaxDocument = documentRepository.save(currentTaxDocument);
        }
        taxDocument.setTest1(isNameCorrect(taxDocument, tenant));
        taxDocument.setTest2(isSalaryCorrect(taxDocument, tenant));
        taxDocument.setFileExtractionType(TaxFileExtractionType.FRANCE_CONNECT);
        documentRepository.updateTaxProcessResult(taxDocument, currentTaxDocument.getId());
    }

    private boolean isSalaryCorrect(TaxDocument taxDocument, Tenant tenant) {
        // TODO : I didn't find ANY relevant result whith this calcul in real use case
        return taxDocument.getAnualSalary() * 0.9 < tenant.getTotalSalary() * 12 && taxDocument.getAnualSalary() * 1.1 > tenant.getTotalSalary();
    }

    private boolean isNameCorrect(TaxDocument taxDocument, Tenant tenant) {
        if (taxDocument.getDeclarant1() != null && taxDocument.getDeclarant1().toLowerCase().contains(tenant.getFirstName().toLowerCase()) &&
                taxDocument.getDeclarant1().toLowerCase().contains(tenant.getLastName().toLowerCase())) {
            return true;
        }
        return taxDocument.getDeclarant1() != null && taxDocument.getDeclarant1().toLowerCase().contains(tenant.getFirstName().toLowerCase()) &&
                taxDocument.getDeclarant1().toLowerCase().contains(tenant.getLastName().toLowerCase());
    }

    private TaxDocument getLatestTaxFromDGFIPByFC(String dgfipBearerToken, String fcToken) {
        String uuid = UUID.randomUUID().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dgfipBearerToken);
        headers.set("X-Correlation-ID", uuid);
        headers.set("X-FranceConnect-OAuth", fcToken);
        headers.set("ID_Teleservice", idTeleservice);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        HttpEntity<?> entity = new HttpEntity<>(null, headers);
        try {
            // TODO automatically check right year, for now we just expect to change on august
            LocalDate currentdate = LocalDate.now();
            String year = String.valueOf(currentdate.getYear() - 1);
            if (currentdate.getMonth().getValue() < 8) {
                year = String.valueOf(currentdate.getYear() - 1);
            }

            String url = dgfipApiUrl + "/impotparticulier/1.0/situations/ir/factures/annrev/" + year;
            ResponseEntity<HashMap> response = restTemplate.exchange(url, HttpMethod.GET, entity, HashMap.class);
            Objects.requireNonNull(response.getBody());
            if (response.getBody() == null) {
                return null;
            }
            TaxDocument taxDocument = new TaxDocument();
            if (response.getBody().get("rfr") != null) {
                int rfr = Integer.parseInt(response.getBody().get("rfr").toString());
                taxDocument.setAnualSalary(rfr);
            }
            if (response.getBody().get("nmNaiDec1") != null && response.getBody().get("prnmDec1") != null) {
                taxDocument.setDeclarant1(response.getBody().get("nmNaiDec1").toString() + " " + response.getBody().get("prnmDec1"));
            }
            if (response.getBody().get("nmNaiDec2") != null && response.getBody().get("prnmDec2") != null) {
                taxDocument.setDeclarant2(response.getBody().get("nmNaiDec2").toString() + " " + response.getBody().get("prnmDec2"));
            }
            return taxDocument;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private String getDGFIPBearerToken() {
        // TODO : we should check that previous token is not expired and keep it
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", dgfipToken);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "RessourceIRFacture");
        HttpEntity<?> entity = new HttpEntity<Object>(body, headers);
        ResponseEntity<HashMap> response = restTemplate.exchange(dgfipApiUrl + "/token", HttpMethod.POST, entity, HashMap.class);
        Objects.requireNonNull(response.getBody());
        return Objects.requireNonNull(response.getBody().get("access_token")).toString();
    }
}
