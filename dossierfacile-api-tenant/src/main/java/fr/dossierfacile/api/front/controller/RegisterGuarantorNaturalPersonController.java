package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonFileForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentProfessionalGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.NameGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
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
@RequestMapping(value = "/api/register/guarantorNaturalPerson", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class RegisterGuarantorNaturalPersonController {
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final AuthenticationFacade authenticationFacade;
    private final LogService logService;

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationGuarantorNaturalPersonForm.tenantId)")
    @PostMapping("/documentIdentification")
    public ResponseEntity<TenantModel> documentIdentification(@Validated(Dossier.class) DocumentIdentificationGuarantorNaturalPersonForm documentIdentificationGuarantorNaturalPersonForm) {
        var tenant = authenticationFacade.getTenant(documentIdentificationGuarantorNaturalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationGuarantorNaturalPersonForm, StepRegister.DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentIdentificationGuarantorNaturalPersonForm.getTenantId() == null)? tenant : authenticationFacade.getTenant(null);
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationGuarantorNaturalPersonFileForm.tenantId)")
    @PostMapping("/documentIdentification/v2")
    public ResponseEntity<TenantModel> documentIdentificationFile(@Validated(Dossier.class) DocumentIdentificationGuarantorNaturalPersonFileForm documentIdentificationGuarantorNaturalPersonFileForm) {
        var tenant = authenticationFacade.getTenant(documentIdentificationGuarantorNaturalPersonFileForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationGuarantorNaturalPersonFileForm, StepRegister.DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON_FILE);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentIdentificationGuarantorNaturalPersonFileForm.getTenantId() == null)? tenant : authenticationFacade.getTenant(null);
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#nameGuarantorNaturalPersonForm.tenantId)")
    @PostMapping("/name")
    public ResponseEntity<TenantModel> guarantorName(NameGuarantorNaturalPersonForm nameGuarantorNaturalPersonForm) throws IllegalAccessException {
        var loggedTenant = authenticationFacade.getTenant(null);
        var tenant = (nameGuarantorNaturalPersonForm.getTenantId() == null) ? loggedTenant :
                loggedTenant.getApartmentSharing().getTenants().stream()
                        .filter(t -> t.getId().equals(nameGuarantorNaturalPersonForm.getTenantId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalAccessException("You are not authorize to see this tenant"));
        var tenantModel = tenantService.saveStepRegister(tenant, nameGuarantorNaturalPersonForm, StepRegister.NAME_GUARANTOR_NATURAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentResidencyGuarantorNaturalPersonForm.tenantId)")
    @PostMapping("/documentResidency")
    public ResponseEntity<TenantModel> documentResidency(@Validated(Dossier.class) DocumentResidencyGuarantorNaturalPersonForm documentResidencyGuarantorNaturalPersonForm) {
        Tenant tenant = authenticationFacade.getTenant(documentResidencyGuarantorNaturalPersonForm.getTenantId());
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentResidencyGuarantorNaturalPersonForm, StepRegister.DOCUMENT_RESIDENCY_GUARANTOR_NATURAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentResidencyGuarantorNaturalPersonForm.getTenantId() == null)? tenant : authenticationFacade.getTenant(null);
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentProfessionalGuarantorNaturalPersonForm.tenantId)")
    @PostMapping("/documentProfessional")
    public ResponseEntity<TenantModel> documentProfessional(@Validated(Dossier.class) DocumentProfessionalGuarantorNaturalPersonForm documentProfessionalGuarantorNaturalPersonForm) {
        var tenant = authenticationFacade.getTenant(documentProfessionalGuarantorNaturalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentProfessionalGuarantorNaturalPersonForm, StepRegister.DOCUMENT_PROFESSIONAL_GUARANTOR_NATURAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentProfessionalGuarantorNaturalPersonForm.getTenantId() == null)? tenant : authenticationFacade.getTenant(null);
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentFinancialGuarantorNaturalPersonForm.tenantId)")
    @PostMapping("/documentFinancial")
    public ResponseEntity<TenantModel> documentFinancial(@Validated(Dossier.class) DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm) {
        var tenant = authenticationFacade.getTenant(documentFinancialGuarantorNaturalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentFinancialGuarantorNaturalPersonForm, StepRegister.DOCUMENT_FINANCIAL_GUARANTOR_NATURAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentFinancialGuarantorNaturalPersonForm.getTenantId() == null)? tenant : authenticationFacade.getTenant(null);
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }

    @PreAuthorize("hasPermissionOnTenant(#documentTaxGuarantorNaturalPersonForm.tenantId)")
    @PostMapping("/documentTax")
    public ResponseEntity<TenantModel> documentTax(@Validated(Dossier.class) DocumentTaxGuarantorNaturalPersonForm documentTaxGuarantorNaturalPersonForm) {
        var tenant = authenticationFacade.getTenant(documentTaxGuarantorNaturalPersonForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentTaxGuarantorNaturalPersonForm, StepRegister.DOCUMENT_TAX_GUARANTOR_NATURAL_PERSON);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        Tenant loggedTenant = (documentTaxGuarantorNaturalPersonForm.getTenantId() == null)? tenant : authenticationFacade.getTenant(null);
        return ok(tenantMapper.toTenantModel(loggedTenant));
    }
}
