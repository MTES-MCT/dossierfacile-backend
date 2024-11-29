package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.guarantor.organism.DocumentGuaranteeProviderCertificateForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.entity.Tenant;
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
@RequestMapping(value = "/api/register/guarantorOrganism", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class RegisterGuarantorOrganismController {

    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final AuthenticationFacade authenticationFacade;

    @PreAuthorize("hasPermissionOnTenant(#form.tenantId)")
    @PostMapping("/documentCertificate")
    public ResponseEntity<TenantModel> documentCertificate(@Validated(Dossier.class) DocumentGuaranteeProviderCertificateForm form) {
        var tenant = authenticationFacade.getTenant(form.getTenantId());
        tenantService.saveStepRegister(tenant, form, StepRegister.DOCUMENT_GUARANTEE_PROVIDER_CERTIFICATE);
        Tenant loggedTenant = (form.getTenantId() == null) ? tenant : authenticationFacade.getLoggedTenant();
        return ok(tenantMapper.toTenantModel(loggedTenant, null));
    }

}
