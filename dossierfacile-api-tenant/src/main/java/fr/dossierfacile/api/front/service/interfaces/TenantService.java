package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.api.front.form.ShareFileByLinkForm;
import fr.dossierfacile.api.front.form.ShareFileByMailForm;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.TenantUpdate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantService {
    <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step);

    void updateLastLoginDateAndResetWarnings(Tenant tenant);

    Tenant create(Tenant tenant);

    Tenant findById(Long coTenantId);

    Tenant findByKeycloakId(String keycloakId);

    Tenant registerFromKeycloakUser(KeycloakUser kcUser, String partner, AcquisitionData acquisitionData);

    Optional<Tenant> findByEmail(String email);

    List<TenantUpdate> findTenantUpdateByCreatedAndPartner(LocalDateTime from, UserApi userApi, Long limit);

    List<TenantUpdate> findTenantUpdateByLastUpdateAndPartner(LocalDateTime from, UserApi userApi, Long limit, boolean includeDeleted, boolean includeRevoked);

    void sendFileByMail(Tenant tenant, ShareFileByMailForm form);

    String createSharingLink(Tenant tenant, ShareFileByLinkForm form);

    void doNotArchive(String token);

    void addCommentAnalysis(Tenant tenant, Long documentId, String comment);

    void resendLink(Long id, Tenant tenant);
}
