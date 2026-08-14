package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HonorDeclaration.class})
class HonorDeclarationTest {

    @MockitoBean
    private TenantCommonRepository tenantRepository;
    @MockitoBean
    private TenantMapper tenantMapper;
    @MockitoBean
    private MailService mailService;
    @MockitoBean
    private TenantStatusService tenantStatusService;
    @MockitoBean
    private ApartmentSharingService apartmentSharingService;
    @MockitoBean
    private TenantMapperForMail tenantMapperForMail;
    @MockitoBean
    private ClientAuthenticationFacade clientAuthenticationFacade;
    @MockitoBean
    private LogService logService;

    @Autowired
    private HonorDeclaration honorDeclaration;

    private Tenant buildTenant(long id, ApartmentSharing apartmentSharing, Guarantor... guarantors) {
        Tenant tenant = Tenant.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Roe")
                .guarantors(new ArrayList<>(List.of(guarantors)))
                .apartmentSharing(apartmentSharing)
                .build();
        apartmentSharing.getTenants().add(tenant);
        for (Guarantor guarantor : guarantors) {
            guarantor.setTenant(tenant);
        }
        when(tenantRepository.findOneById(id)).thenReturn(tenant);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return tenant;
    }

    private static ApartmentSharing apartmentSharing(ApplicationType applicationType) {
        return ApartmentSharing.builder()
                .applicationType(applicationType)
                .tenants(new ArrayList<>())
                .build();
    }

    private static Guarantor naturalGuarantor(long id, String email) {
        return Guarantor.builder()
                .id(id)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .build();
    }

    private static HonorDeclarationForm form() {
        HonorDeclarationForm form = new HonorDeclarationForm();
        form.setHonorDeclaration(true);
        return form;
    }

    private void saveStepAndCommit(Tenant tenant) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            honorDeclaration.saveStep(tenant, form());
            // Simulate the transaction commit that triggers afterCommit callbacks
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void should_notify_natural_person_guarantors_on_first_submission() {
        Guarantor guarantor = naturalGuarantor(10L, "guarantor@dossierfacile.fr");
        Tenant tenant = buildTenant(1L, apartmentSharing(ApplicationType.ALONE), guarantor);

        saveStepAndCommit(tenant);

        verify(mailService, times(1)).sendEmailToGuarantor(eq("guarantor@dossierfacile.fr"), eq("John Doe"), eq(tenant));
        verify(logService, times(1)).saveGuarantorNotifiedLog(guarantor);
    }

    /**
     * Guarantors already notified at first submission must not be notified again when
     * /honorDeclaration is called back (clarification, resubmission after a BO refusal).
     * An email added or corrected after submission is handled by NameGuarantorNaturalPerson.
     */
    @Test
    void should_not_notify_guarantors_again_on_resubmission() {
        Guarantor guarantor = naturalGuarantor(11L, "guarantor@dossierfacile.fr");
        Tenant tenant = buildTenant(1L, apartmentSharing(ApplicationType.ALONE), guarantor);
        tenant.setHonorDeclaration(true);

        saveStepAndCommit(tenant);

        verify(mailService, never()).sendEmailToGuarantor(any(), any(), any());
        verify(logService, never()).saveGuarantorNotifiedLog(any());
    }

    @Test
    void should_not_notify_guarantors_without_email_or_of_another_type() {
        Guarantor withoutEmail = naturalGuarantor(12L, null);
        Guarantor legalPerson = Guarantor.builder()
                .id(13L)
                .typeGuarantor(TypeGuarantor.LEGAL_PERSON)
                .firstName("John")
                .legalPersonName("Garantme")
                .email("legal@dossierfacile.fr")
                .build();
        Tenant tenant = buildTenant(1L, apartmentSharing(ApplicationType.ALONE), withoutEmail, legalPerson);

        saveStepAndCommit(tenant);

        verify(mailService, never()).sendEmailToGuarantor(any(), any(), any());
        verify(logService, never()).saveGuarantorNotifiedLog(any());
    }

    @Test
    void should_notify_guarantors_of_both_partners_for_couple() {
        ApartmentSharing apartmentSharing = apartmentSharing(ApplicationType.COUPLE);
        Guarantor guarantor = naturalGuarantor(14L, "guarantor@dossierfacile.fr");
        Guarantor partnerGuarantor = naturalGuarantor(15L, "partner-guarantor@dossierfacile.fr");
        Tenant tenant = buildTenant(1L, apartmentSharing, guarantor);
        Tenant partner = buildTenant(2L, apartmentSharing, partnerGuarantor);

        saveStepAndCommit(tenant);

        verify(mailService, times(1)).sendEmailToGuarantor(eq("guarantor@dossierfacile.fr"), eq("John Doe"), eq(tenant));
        verify(mailService, times(1)).sendEmailToGuarantor(eq("partner-guarantor@dossierfacile.fr"), eq("John Doe"), eq(partner));
        verify(logService, times(1)).saveGuarantorNotifiedLog(guarantor);
        verify(logService, times(1)).saveGuarantorNotifiedLog(partnerGuarantor);
    }
}
