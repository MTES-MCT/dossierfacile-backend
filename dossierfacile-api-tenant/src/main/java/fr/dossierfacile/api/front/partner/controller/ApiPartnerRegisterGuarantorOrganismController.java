package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.guarantor.organism.DocumentIdentificationGuarantorOrganismForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.DocumentIdentificationGuarantor;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final LogService logService;

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationGuarantorOrganismForm.tenantId)")
    @PostMapping("/documentIdentification")
    public ResponseEntity<TenantModel> documentIdentification(@Validated({ApiPartner.class, DocumentIdentificationGuarantor.class}) DocumentIdentificationGuarantorOrganismForm documentIdentificationGuarantorOrganismForm) {
        var tenant = tenantService.findById(documentIdentificationGuarantorOrganismForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationGuarantorOrganismForm, StepRegister.DOCUMENT_IDENTIFICATION_GUARANTOR_ORGANISM);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }
}
