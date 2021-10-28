package fr.gouv.owner.controller;


import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.owner.dto.OwnerDTO;
import fr.gouv.owner.dto.ProspectDTO;
import fr.gouv.owner.repository.OwnerRepository;
import fr.gouv.owner.repository.PropertyRepository;
import fr.gouv.owner.repository.UserRepository;
import fr.gouv.owner.security.SubscriptionStatus;
import fr.gouv.owner.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;

@Controller
@Slf4j
public class OwnerController {

    private static final String TOKEN = "token";
    private static final String OWNER = "owner";
    private static final String REDIRECT_ERROR = "redirect:/error";
    private static final String REDIRECT_PROFILE = "redirect:/proprietaire/mon-compte";
    private static final String REDIRECT_LOGIN = "redirect:/login";

    @Autowired
    private OwnerService ownerService;
    @Autowired
    private UserService userService;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private ProspectService prospectService;
    @Autowired
    private TenantService tenantService;

    @GetMapping("/")
    public String infoOwner(Model model, @RequestParam(value = "token", required = false) String token) {
        OwnerDTO owner = new OwnerDTO();
        model.addAttribute(OWNER, owner);
        model.addAttribute(TOKEN, token);
        return "info-proprietaire";
    }

    @GetMapping("/home")
    public ModelAndView index() {
        return new ModelAndView("redirect:/");
    }

    @GetMapping("/proprietaire/contacter/{token}")
    public String subscribeOwner(Model model, @PathVariable("token") String token, Principal principal, HttpServletRequest request) {
        Property property = propertyService.findOneByToken(token);
        if (null == property) {
            return REDIRECT_ERROR;
        }
        propertyService.addVisit(property);
        request.getSession().setAttribute("ownerToken", token);
        if (principal == null) {
            return "redirect:/proprietaire/contacter/info";
        }
        User user = userService.getUser(principal);
        if (user instanceof Owner) {
            return "redirect:/proprietaire/contacter/info";
        }

        model.addAttribute(TOKEN, token);
        model.addAttribute("status", ownerService.getStatusOwnerLink(principal.getName(), property.getOwner()));
        return "owner-subscriber";
    }

    @GetMapping("/proprietaire/contacter/info")
    public String info(Model model, HttpSession session) {
        String token = (String) session.getAttribute("ownerToken");
        Property property = propertyService.findOneByToken(token);
        if (property != null) {
            model.addAttribute("ownerName", property.getOwner().getFullName());
            model.addAttribute(TOKEN, token);
            model.addAttribute("propertyName", property.getName());
            return "owner-subscriber-info";
        }
        return "redirect:/locataire";
    }

    @PostMapping("/proprietaire/owner-subscriber")
    public String ownerSubscriber(String token, Boolean access, Model model, String tenantId, Principal principal) {
        Tenant tenant = null;
        if (principal != null) {
            tenant = tenantService.findOneByEmail(principal.getName());
        }
        if (tenant == null && tenantId != null) {
            tenant = tenantService.find(Integer.parseInt(tenantId));
        }
        if (tenant == null) {
            return REDIRECT_ERROR;
        }
        SubscriptionStatus subscriptionStatus = ownerService.subscribeTenant(token, access, tenant);
        model.addAttribute("status", subscriptionStatus);
        model.addAttribute("message", "Le propriétaire a bien reçu votre demande de visite");
        return "subscription-result";
    }

    @GetMapping("/proprietaire/mon-compte")
    public String ownerProfile(Model model, Principal principal) {
        Owner owner = ownerService.getOwner(principal);
        if (owner != null && owner.getStepRegisterOwner() == StepRegisterOwner.STEP2) {
            return "redirect:/registerOwner/step3/" + owner.getSlug();
        }
        model.addAttribute(OWNER, owner);
        model.addAttribute("prospectDTO", new ProspectDTO());
        return "owner/owner-profile";
    }

    @PostMapping("/proprietaire/mon-compte")
    public String updateOwnerProfile(@Validated @RequestParam("firstName") String firstName,
                                     @Validated @RequestParam("lastName") String lastName,
                                     Principal principal, RedirectAttributes redirectAttributes) {
        Owner owner = ownerService.getOwner(principal);
        boolean changed = false;
        if (!firstName.trim().equals(owner.getFirstName())) {
            owner.setFirstName(firstName.trim());
            changed = true;
        }
        if (!lastName.trim().equals(owner.getLastName())) {
            owner.setLastName(lastName.trim());
            changed = true;
        }
        if (changed) {
            ownerService.saveOwner(owner);
            redirectAttributes.addFlashAttribute("saved", true);
        }
        return REDIRECT_PROFILE;
    }

    @PostMapping("/proprietaire/supprimer-mon-compte")
    public String deleteOwnerProfile(HttpServletRequest request, Principal principal) {
        Owner owner = ownerService.getOwner(principal);
        ownerService.deleteOwner(owner);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        for (Cookie cookie : request.getCookies()) {
            cookie.setMaxAge(0);
        }
        return "redirect:/";
    }


    @GetMapping("/proprietaire/tuto")
    public String ownerTutorial(Model model, Principal principal) {
        if (!loggedIn(principal))
            return REDIRECT_LOGIN;

        Owner owner = ownerService.getOwner(principal);
        if (owner != null && owner.getStepRegisterOwner() == StepRegisterOwner.STEP2) {
            return "redirect:/registerOwner/step3/" + owner.getSlug();
        }
        model.addAttribute(OWNER, owner);
        model.addAttribute("prospectDTO", new ProspectDTO());
        return "owner/tuto";
    }

    @PostMapping("/proprietaire/prospect")
    public String registerProspect(@Validated @ModelAttribute("prospectDTO") ProspectDTO prospectDTO, BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null || result.hasErrors()) {
            return REDIRECT_ERROR;
        }
        Owner owner = ownerService.getOwner(principal);
        if (owner == null) {
            return REDIRECT_ERROR;
        }
        prospectDTO.setPropertyId(owner.lastProperty().getId());
        prospectService.createProspect(prospectDTO, false);
        redirectAttributes.addFlashAttribute("notification", true);
        return "redirect:/proprietaire/ma-propriete";
    }

    private boolean loggedIn(Principal principal) {
        return principal != null;
    }

    @PostMapping("/proprietaire/removeExample")
    public String removeExample(Principal principal) {
        Owner owner = ownerService.findByEmail(principal.getName());
        if (owner == null) {
            return REDIRECT_ERROR;
        }
        owner.setExample(false);
        ownerService.save(owner);
        return "redirect:/proprietaire/ma-propriete";
    }
}