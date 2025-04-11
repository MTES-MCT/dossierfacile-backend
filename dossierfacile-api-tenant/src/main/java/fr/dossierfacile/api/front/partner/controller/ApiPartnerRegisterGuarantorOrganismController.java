package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.guarantor.organism.DocumentGuaranteeProviderCertificateForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api-partner/register/guarantorOrganism", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPartnerRegisterGuarantorOrganismController {

    private final TenantService tenantService;

    @PreAuthorize("hasPermissionOnTenant(#form.tenantId)")
    @PostMapping("/documentCertificate")
    @Transactional
    public ResponseEntity<TenantModel> documentCertificate(@Validated({ApiPartner.class}) DocumentGuaranteeProviderCertificateForm form) {
        var tenant = tenantService.findById(form.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, form, StepRegister.DOCUMENT_GUARANTEE_PROVIDER_CERTIFICATE);
        return ok(tenantModel);
    }

}
