package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.DocumentIdentificationGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.DocumentIdentificationRepresentanGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.NameGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.NameGuarantorRepresentantLegalPersonForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.DocumentIdentificationGuarantor;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.entity.Tenant;
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
@RequestMapping(value = "/api/register/guarantorLegalPerson", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class RegisterGuarantorLegalPersonController {
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final AuthenticationFacade authenticationFacade;
    private final LogService logService;

    @PreAuthorize("hasPermissionOnTenant(#nameGuarantorLegalPersonForm.tenantId)")
    @PostMapping("/name")
    public ResponseEntity<TenantModel> guarantorName(@Validated(Dossier.class) NameGuarantorLegalPersonForm nameGuarantorLegalPersonForm) {
        var tenant = authenticationFacade.getTenant(nameGuarantorLegalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, nameGuarantorLegalPersonForm, StepRegister.NAME_GUARANTOR_LEGAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (nameGuarantorLegalPersonForm.getTenantId() == null) ? tenant : authenticationFacade.getLoggedTenant();
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationGuarantorLegalPersonForm.tenantId)")
    @PostMapping("/documentIdentification")
    public ResponseEntity<TenantModel> documentIdentification(@Validated({Dossier.class, DocumentIdentificationGuarantor.class}) DocumentIdentificationGuarantorLegalPersonForm documentIdentificationGuarantorLegalPersonForm) {
        Tenant tenant = authenticationFacade.getTenant(documentIdentificationGuarantorLegalPersonForm.getTenantId());
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationGuarantorLegalPersonForm, StepRegister.DOCUMENT_IDENTIFICATION_GUARANTOR_LEGAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentIdentificationGuarantorLegalPersonForm.getTenantId() == null) ? tenant : authenticationFacade.getLoggedTenant();
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationRepresentantGuarantorLegalPersonForm.tenantId)")
    @PostMapping("/documentRepresentantIdentification")
    public ResponseEntity<TenantModel> documentIdentificationRepresentant(@Validated({Dossier.class, DocumentIdentificationGuarantor.class}) DocumentIdentificationRepresentanGuarantorLegalPersonForm documentIdentificationRepresentantGuarantorLegalPersonForm) {
        Tenant tenant = authenticationFacade.getTenant(documentIdentificationRepresentantGuarantorLegalPersonForm.getTenantId());
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationRepresentantGuarantorLegalPersonForm, StepRegister.DOCUMENT_IDENTIFICATION_REPRESENTANT_GUARANTOR_LEGAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentIdentificationRepresentantGuarantorLegalPersonForm.getTenantId() == null) ? tenant : authenticationFacade.getLoggedTenant();
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#nameGuarantorRepresentantLegalPersonForm.tenantId)")
    @PostMapping("/representing-name")
    public ResponseEntity<TenantModel> guarantorRepresentingName(@Validated(Dossier.class) NameGuarantorRepresentantLegalPersonForm nameGuarantorRepresentantLegalPersonForm) {
        var tenant = authenticationFacade.getTenant(nameGuarantorRepresentantLegalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, nameGuarantorRepresentantLegalPersonForm, StepRegister.NAME_IDENTIFICATION_REPRESENTANT_GUARANTOR_LEGAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (nameGuarantorRepresentantLegalPersonForm.getTenantId() == null) ? tenant : authenticationFacade.getLoggedTenant();
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }
}
