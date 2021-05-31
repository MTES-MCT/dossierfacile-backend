package fr.dossierfacile.api.front.controller;

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
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/register")
public class RegisterController {

    private final TenantService tenantService;
    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;
    private final LogService logService;

    @PostMapping("/account")
    public ResponseEntity<TenantModel> account(@Validated @RequestBody AccountForm accountForm) {
        TenantModel tenantModel = tenantService.saveStepRegister(null, accountForm, StepRegister.ACCOUNT);
        logService.saveLog(LogType.ACCOUNT_CREATED,tenantModel.getId());
        return ok(tenantModel);
    }

    @GetMapping("/confirmAccount/{token}")
    public ResponseEntity<Void> confirmAccount(@PathVariable String token) {
        userService.confirmAccount(token);
        return ok().build();
    }

    @PostMapping("/names")
    public ResponseEntity<TenantModel> names(@Validated @RequestBody NamesForm namesForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, namesForm, StepRegister.NAMES);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/application")
    public ResponseEntity<TenantModel> application(@Validated @RequestBody ApplicationForm applicationForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, applicationForm, StepRegister.APPLICATION);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/honorDeclaration")
    public ResponseEntity<TenantModel> honorDeclaration(@Validated @RequestBody HonorDeclarationForm honorDeclarationForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, honorDeclarationForm, StepRegister.HONOR_DECLARATION);
        logService.saveLog(LogType.ACCOUNT_COMPLETED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentIdentification")
    public ResponseEntity<TenantModel> documentIdentification(@Validated DocumentIdentificationForm documentIdentificationForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationForm, StepRegister.DOCUMENT_IDENTIFICATION);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentResidency")
    public ResponseEntity<TenantModel> documentResidency(@Validated DocumentResidencyForm documentResidencyForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentResidencyForm, StepRegister.DOCUMENT_RESIDENCY);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentProfessional")
    public ResponseEntity<TenantModel> step4DocumentProfessional(@Validated DocumentProfessionalForm documentProfessionalForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentProfessionalForm, StepRegister.DOCUMENT_PROFESSIONAL);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentFinancial")
    public ResponseEntity<TenantModel> documentFinancial(@Validated DocumentFinancialForm documentFinancialForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentFinancialForm, StepRegister.DOCUMENT_FINANCIAL);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/documentTax")
    public ResponseEntity<TenantModel> documentTax(@Validated DocumentTaxForm documentTaxForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, documentTaxForm, StepRegister.DOCUMENT_TAX);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }

    @PostMapping("/guarantorType")
    public ResponseEntity<TenantModel> guarantor(@Validated @RequestBody GuarantorTypeForm guarantorTypeForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        TenantModel tenantModel = tenantService.saveStepRegister(tenant, guarantorTypeForm, StepRegister.GUARANTOR_TYPE);
        logService.saveLog(LogType.ACCOUNT_EDITED,tenantModel.getId());
        return ok(tenantModel);
    }
}
