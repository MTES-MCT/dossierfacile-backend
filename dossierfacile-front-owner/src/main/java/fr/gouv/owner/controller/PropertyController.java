package fr.gouv.owner.controller;


import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Prospect;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.owner.dto.PropertyApartmentSharingDTO;
import fr.gouv.owner.dto.PropertyDTO;
import fr.gouv.owner.dto.ProspectDTO;
import fr.gouv.owner.service.OwnerService;
import fr.gouv.owner.service.PropertyApartmentSharingService;
import fr.gouv.owner.service.PropertyService;
import fr.gouv.owner.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@Slf4j
public class PropertyController {

    private static final String OWNER = "owner";
    private static final String PROPERTY = "property";
    private static final String TENANT_LIST = "tenantsList";
    private static final String PROPERTY_LIST = "propertyList";
    private static final String PROPERTY_APT = "propertyApt";
    private static final String PROSPECT_DTO = "prospectDTO";
    private static final String REDIRECT_PROFILE = "redirect:/proprietaire/mon-compte";
    private static final String REDIRECT_LOGIN = "redirect:/login";

    @Autowired
    private PropertyService propertyService;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private PropertyApartmentSharingService propertyApartmentSharingService;
    @Autowired
    private TenantService tenantService;

    @GetMapping("/proprietaire/ajouter-propriete")
    public String showCreatePropertyForm(Model model, Principal principal) {
        if (notLoggedIn(principal))
            return REDIRECT_LOGIN;
        Owner owner = ownerService.getOwner(principal);
        model.addAttribute(OWNER, owner);

        model.addAttribute(PROPERTY, new PropertyDTO());
        return "owner/add-property";
    }

    @GetMapping("/proprietaire/ma-propriete")
    public String ownerProperty(Model model, Principal principal) {
        Owner owner = ownerService.getOwner(principal);
        if (owner == null) {
            return "redirect:/error";
        }

        if (owner.getStepRegisterOwner() == StepRegisterOwner.STEP2) {
            return "redirect:/registerOwner/step3/" + owner.getSlug();
        }
        Property property = owner.lastProperty();
        if (property == null) {
            return "redirect:/error";
        }
        List<Prospect> prospectList = new ArrayList<>();
        int size = property.getProspects().size();
        for (int i = 0; i < size; i++) {
            Prospect prospect = property.getProspects().get(i);
            if (prospect.getProspectType() != null && prospect.getProspectType() == TenantType.CREATE) {
                prospectList.add(prospect);
            }
        }
        List<Property> propertyList = propertyService.getPropertyList(owner);
        List<PropertyApartmentSharing> propertyApartmentSharingList = propertyApartmentSharingService.getAllPropertyApartmentSharing(propertyList);
        List<Tenant> tenantsList = new ArrayList<>();
        for (PropertyApartmentSharing propertyApartmentSharing : propertyApartmentSharingList) {
            List<Tenant> tenantList = tenantService.getAllTenantByApartmentSharing(propertyApartmentSharing);
            for (Tenant ten : tenantList) {
                if (ten.getTenantType() == TenantType.CREATE) {
                    tenantsList.add(ten);
                }
            }
        }
        PropertyApartmentSharingDTO propertyApartmentSharingDTO = new PropertyApartmentSharingDTO();
        propertyApartmentSharingDTO.setPropertyApartmentSharingList(propertyApartmentSharingList);

        tenantsList.sort(Comparator.comparing(Tenant::getCreationDateTime).reversed());

        model.addAttribute(TENANT_LIST, tenantsList);
        model.addAttribute(PROPERTY_LIST, propertyApartmentSharingList);
        model.addAttribute(PROPERTY, property);
        model.addAttribute(OWNER, owner);
        model.addAttribute(PROPERTY_APT, propertyApartmentSharingDTO);
        model.addAttribute(PROSPECT_DTO, new ProspectDTO());
        return "owner/see-property";
    }

    private boolean notLoggedIn(Principal principal) {
        return principal == null;
    }

    @PostMapping("/property/{id}/apartmentSharing")
    public String joinTenantToProperty(@PathVariable Long id, Principal principal, @RequestParam String url) {
        Owner owner = ownerService.getOwner(principal);
        Property property = propertyService.findOne(id);
        if (!property.getOwner().getId().equals(owner.getId())) {
            return REDIRECT_PROFILE;
        }
        ownerService.linkOwnerTenant(property, extractToken(url));
        return REDIRECT_PROFILE;
    }

    private String extractToken(String url) {
        String[] result = url.split("/");
        return result[result.length - 1];
    }

    @PostMapping("/proprietaire/tenant/{id}")
    public String removeTenant(@PathVariable("id") Long id, Principal principal) {

        Owner owner = ownerService.getOwner(principal);
        List<Property> propertyList = propertyService.getPropertyList(owner);

        List<PropertyApartmentSharing> propertyApartmentSharingList = propertyApartmentSharingService.getAllPropertyApartmentSharing(propertyList);
        PropertyApartmentSharing propertyApartmentSharing1 = propertyApartmentSharingList.stream().filter(propertyApartmentSharing ->
                propertyApartmentSharing.getApartmentSharing().getId().equals(id)).findAny().orElse(null);

        assert propertyApartmentSharing1 != null;
        propertyApartmentSharingService.deletePropertyApartmentSharing(propertyApartmentSharing1);

        return "redirect:/proprietaire/ma-propriete";
    }
}
