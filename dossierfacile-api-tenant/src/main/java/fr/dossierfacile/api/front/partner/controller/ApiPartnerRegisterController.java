package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.partner.AccountPartnerForm;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentIdentificationForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.register.form.tenant.GuarantorTypeForm;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.service.interfaces.LogService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/api-partner/register", produces = MediaType.APPLICATION_JSON_VALUE)
@MethodLogTime
public class ApiPartnerRegisterController {

    private final TenantService tenantService;
    private final LogService logService;

    @PostMapping(value = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> account(@Validated(ApiPartner.class) @RequestBody AccountPartnerForm accountForm) {
        var tenantModel = tenantService.saveStepRegister(null, accountForm, StepRegister.ACCOUNT_PARTNER_API);
        logService.saveLog(LogType.ACCOUNT_CREATED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#namesForm.tenantId)")
    @PostMapping(value = "/names", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> names(@Validated(ApiPartner.class) @RequestBody NamesForm namesForm) {
        var tenant = tenantService.findById(namesForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, namesForm, StepRegister.NAMES);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @ApiOperation("Deprecated since 2022.09.15 - use /application/v2")
    @PreAuthorize("hasPermissionOnTenant(#applicationForm.tenantId)")
    @Deprecated
    @PostMapping(value = "/application", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> application(@Validated(ApiPartner.class) @RequestBody ApplicationForm applicationForm) {
        var tenant = tenantService.findById(applicationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, applicationForm, StepRegister.APPLICATION_V1);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#applicationForm.tenantId)")
    @PostMapping(value = "/application/v2", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> application(@Validated(ApiPartner.class) @RequestBody ApplicationFormV2 applicationForm) {
        var tenant = tenantService.findById(applicationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, applicationForm, StepRegister.APPLICATION);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#honorDeclarationForm.tenantId)")
    @PostMapping(value = "/honorDeclaration", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> honorDeclaration(@Validated(ApiPartner.class) @RequestBody HonorDeclarationForm honorDeclarationForm) {
        var tenant = tenantService.findById(honorDeclarationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, honorDeclarationForm, StepRegister.HONOR_DECLARATION);
        logService.saveLog(LogType.ACCOUNT_COMPLETED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentIdentificationForm.tenantId)")
    @PostMapping(value = "/documentIdentification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantModel> documentIdentification(@Validated(ApiPartner.class) DocumentIdentificationForm documentIdentificationForm) {
        var tenant = tenantService.findById(documentIdentificationForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentIdentificationForm, StepRegister.DOCUMENT_IDENTIFICATION);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentResidencyForm.tenantId)")
    @PostMapping(value = "/documentResidency", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantModel> documentResidency(@Validated(ApiPartner.class) DocumentResidencyForm documentResidencyForm) {
        var tenant = tenantService.findById(documentResidencyForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentResidencyForm, StepRegister.DOCUMENT_RESIDENCY);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentProfessionalForm.tenantId)")
    @PostMapping(value = "/documentProfessional", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantModel> documentProfessional(@Validated(ApiPartner.class) DocumentProfessionalForm documentProfessionalForm) {
        var tenant = tenantService.findById(documentProfessionalForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentProfessionalForm, StepRegister.DOCUMENT_PROFESSIONAL);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentFinancialForm.tenantId)")
    @PostMapping(value = "/documentFinancial", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantModel> documentFinancial(@Validated(ApiPartner.class) DocumentFinancialForm documentFinancialForm) {
        var tenant = tenantService.findById(documentFinancialForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentFinancialForm, StepRegister.DOCUMENT_FINANCIAL);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#documentTaxForm.tenantId)")
    @PostMapping(value = "/documentTax", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantModel> documentTax(@Validated(ApiPartner.class) DocumentTaxForm documentTaxForm) {
        var tenant = tenantService.findById(documentTaxForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, documentTaxForm, StepRegister.DOCUMENT_TAX);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }

    @PreAuthorize("hasPermissionOnTenant(#guarantorTypeForm.tenantId)")
    @PostMapping(value = "/guarantorType", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> guarantor(@Validated(ApiPartner.class) @RequestBody GuarantorTypeForm guarantorTypeForm) {
        var tenant = tenantService.findById(guarantorTypeForm.getTenantId());
        var tenantModel = tenantService.saveStepRegister(tenant, guarantorTypeForm, StepRegister.GUARANTOR_TYPE);
        logService.saveLog(LogType.ACCOUNT_EDITED, tenantModel.getId());
        return ok(tenantModel);
    }
}
