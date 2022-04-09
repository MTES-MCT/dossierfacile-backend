package fr.gouv.owner.controller;


import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.dto.ProspectDTO;
import fr.gouv.owner.service.PropertyService;
import fr.gouv.owner.service.ProspectService;
import java.security.Principal;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class ProspectController {

    private static final String PROSPECT_CONTROLLER_ERROR = "ProspectController registerProspect errors: {}";
    private static final String REDIRECT_ERROR = "redirect:/error";
    private static final String INVALID_PROSPECT = "Invalid prospect id";
    private static final String OWNER_TOKEN = "ownerToken";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String EMAIL = "email";

    @Autowired
    private ProspectService prospectService;
    @Autowired
    private PropertyService propertyService;

    @PostMapping("/prospect")
    public String registerProspect(@Validated @ModelAttribute("prospectDTO") ProspectDTO prospectDTO, BindingResult result, HttpSession session) {
        if (result.hasErrors()) {
            log.error(PROSPECT_CONTROLLER_ERROR, result.getAllErrors());
            return REDIRECT_ERROR;
        }
        Prospect prospect = prospectService.createProspect(prospectDTO, true);
        if (prospectDTO.isBooking()) {
            session.setAttribute("bookingProspectId", prospect.getId());
            return "redirect:/agent/prospect/" + prospect.getId();
        }
        return "redirect:/agent/prospect/" + prospect.getId();
    }

    @PostMapping("/prospect/{id}/delete")
    public String delete(@PathVariable Long id) {
        Prospect prospect = prospectService.find(id);
        if (prospect == null) {
            log.error(PROSPECT_CONTROLLER_ERROR, INVALID_PROSPECT);
            return REDIRECT_ERROR;
        }
        prospectService.delete(id);
        return "redirect:/proprietaire/ma-propriete";
    }

    @PostMapping("/prospect/customMessage/{id}")
    public String customMessage(ProspectDTO prospectDTO, @PathVariable Long id, Principal principal) {
        Prospect prospect = prospectService.find(id);
        if (prospect == null) {
            log.error(PROSPECT_CONTROLLER_ERROR, INVALID_PROSPECT);
            return REDIRECT_ERROR;
        }
        prospectService.saveCustomMessage(prospect, prospectDTO);
        return "redirect:/proprietaire/ma-propriete";
    }

    @GetMapping("/prospect/invite/{token}")
    public String inviteProspect(@PathVariable String token,
                                 @RequestParam String email,
                                 @RequestParam(value = "firstName", required = false) String firstName,
                                 @RequestParam(value = "lastName", required = false) String lastName,
                                 HttpSession session) {
        Property property = propertyService.findOneByToken(token);
        if (property == null) {
            return REDIRECT_ERROR;
        }
        session.setAttribute(OWNER_TOKEN, token);
        session.setAttribute(FIRST_NAME, firstName);
        session.setAttribute(LAST_NAME, lastName);
        session.setAttribute(EMAIL, email);
        return "redirect:/registerTenant/step1";
    }

}