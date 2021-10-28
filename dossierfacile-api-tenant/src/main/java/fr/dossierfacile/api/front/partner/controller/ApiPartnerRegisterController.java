package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentIdentificationForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.register.form.tenant.GuarantorTypeForm;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/register")
public class ApiPartnerRegisterController {

    private final TenantService tenantService;
    private final AuthenticationFacade authenticationFacade;
    private final LogService logService;

    @PostMapping("/account")
    public ResponseEntity<TenantModel> account(@Validated(ApiPartner.class) @RequestBody AccountForm accountForm) {
        var tenantModel = tenantService.saveStepRegister(null, accountForm, StepRegister.ACCOUNT_PARTNER_API);
        logService.saveLog(LogType.ACCOUNT_CREATED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/names")
    public ResponseEntity<TenantModel> names(@Validated(ApiPartner.class) @RequestBody NamesForm namesForm) {
        var tenant = authenticationFacade.getTenant(namesForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, namesForm, StepRegister.NAMES);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/application")
    public ResponseEntity<TenantModel> application(@Validated(ApiPartner.class) @RequestBody ApplicationForm applicationForm) {
        var tenant = authenticationFacade.getTenant(applicationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, applicationForm, StepRegister.APPLICATION);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/honorDeclaration")
    public ResponseEntity<TenantModel> honorDeclaration(@Validated(ApiPartner.class) @RequestBody HonorDeclarationForm honorDeclarationForm) {
        var tenant = authenticationFacade.getTenant(honorDeclarationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, honorDeclarationForm, StepRegister.HONOR_DECLARATION);
        logService.saveLog(LogType.ACCOUNT_COMPLETED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentIdentification")
    public ResponseEntity<TenantModel> documentIdentification(@Validated(ApiPartner.class) DocumentIdentificationForm documentIdentificationForm) {
        var tenant = authenticationFacade.getTenant(documentIdentificationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationForm, StepRegister.DOCUMENT_IDENTIFICATION);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentResidency")
    public ResponseEntity<TenantModel> documentResidency(@Validated(ApiPartner.class) DocumentResidencyForm documentResidencyForm) {
        var tenant = authenticationFacade.getTenant(documentResidencyForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentResidencyForm, StepRegister.DOCUMENT_RESIDENCY);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentProfessional")
    public ResponseEntity<TenantModel> documentProfessional(@Validated(ApiPartner.class) DocumentProfessionalForm documentProfessionalForm) {
        var tenant = authenticationFacade.getTenant(documentProfessionalForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentProfessionalForm, StepRegister.DOCUMENT_PROFESSIONAL);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentFinancial")
    public ResponseEntity<TenantModel> documentFinancial(@Validated(ApiPartner.class) DocumentFinancialForm documentFinancialForm) {
        var tenant = authenticationFacade.getTenant(documentFinancialForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentFinancialForm, StepRegister.DOCUMENT_FINANCIAL);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentTax")
    public ResponseEntity<TenantModel> documentTax(@Validated(ApiPartner.class) DocumentTaxForm documentTaxForm) {
        var tenant = authenticationFacade.getTenant(documentTaxForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentTaxForm, StepRegister.DOCUMENT_TAX);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/guarantorType")
    public ResponseEntity<TenantModel> guarantor(@Validated(ApiPartner.class) @RequestBody GuarantorTypeForm guarantorTypeForm) {
        var tenant = authenticationFacade.getTenant(guarantorTypeForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, guarantorTypeForm, StepRegister.GUARANTOR_TYPE);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }
}
