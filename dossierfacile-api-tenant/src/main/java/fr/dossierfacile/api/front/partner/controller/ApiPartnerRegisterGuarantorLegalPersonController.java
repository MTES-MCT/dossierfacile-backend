package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.DocumentIdentificationGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.DocumentIdentificationRepresentanGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.NameGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.DocumentIdentificationGuarantor;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.service.interfaces.LogService;
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
@RequestMapping(value = "/api-partner/register/guarantorLegalPerson", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPartnerRegisterGuarantorLegalPersonController {
    private final TenantService tenantService;
    private final LogService logService;

    @PreAuthorize("hasPermissionOnTenant(#nameGuarantorLegalPersonForm.tenantId)")
    @PostMapping("/name")
    @Transactional
    public ResponseEntity<TenantModel> guarantorName(@Validated(ApiPartner.class) NameGuarantorLegalPersonForm nameGuarantorLegalPersonForm) {
        var tenant = tenantService.findById(nameGuarantorLegalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, nameGuarantorLegalPersonForm, StepRegister.NAME_GUARANTOR_LEGAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationGuarantorLegalPersonForm.tenantId)")
    @PostMapping("/documentIdentification")
    @Transactional
    public ResponseEntity<TenantModel> documentIdentification(@Validated({ApiPartner.class, DocumentIdentificationGuarantor.class}) DocumentIdentificationGuarantorLegalPersonForm documentIdentificationGuarantorLegalPersonForm) {
        var tenant = tenantService.findById(documentIdentificationGuarantorLegalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationGuarantorLegalPersonForm, StepRegister.DOCUMENT_IDENTIFICATION_GUARANTOR_LEGAL_PERSON);
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationRepresentanGuarantorLegalPersonForm.tenantId)")
    @PostMapping("/documentRepresentantIdentification")
    @Transactional
    public ResponseEntity<TenantModel> documentIdentificationRepresentant(@Validated({ApiPartner.class, DocumentIdentificationGuarantor.class}) DocumentIdentificationRepresentanGuarantorLegalPersonForm documentIdentificationRepresentanGuarantorLegalPersonForm) {
        var tenant = tenantService.findById(documentIdentificationRepresentanGuarantorLegalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationRepresentanGuarantorLegalPersonForm, StepRegister.DOCUMENT_IDENTIFICATION_REPRESENTANT_GUARANTOR_LEGAL_PERSON);
        return ok(tenantModel);
    }
}
