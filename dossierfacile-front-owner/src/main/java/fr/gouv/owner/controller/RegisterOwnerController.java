package fr.gouv.owner.controller;


import fr.gouv.owner.dto.OwnerDTO;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.owner.service.OwnerService;
import fr.gouv.owner.validator.interfaces.Step1RegisterOwner;
import fr.gouv.owner.validator.interfaces.Step2RegisterOwner;
import fr.gouv.owner.validator.interfaces.Step3RegisterOwner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = "/registerOwner")
@Slf4j
public class RegisterOwnerController {
    private static final String OWNER_DTO = "ownerDTO";
    @Autowired
    private OwnerService ownerService;

    @GetMapping("/step1")
    public String step1Form(Model model) {
        OwnerDTO ownerDTO = new OwnerDTO();
        model.addAttribute(OWNER_DTO, ownerDTO);
        return "registerOwner/step1";
    }

    @PostMapping("/step1")
    public String step1ProcessForm(@Validated(Step1RegisterOwner.class) @ModelAttribute OwnerDTO ownerDTO, BindingResult result) {
        if (result.hasErrors()) {
            return "register/step1";
        }
        Owner owner = ownerService.saveStep(ownerDTO, StepRegisterOwner.STEP1);
        return "redirect:/registerOwner/step2/" + owner.getSlug();
    }

    @GetMapping("/step2/{slug}")
    public String step2Form(@PathVariable String slug, Model model) {
        Owner owner = ownerService.findBySlug(slug);
        if (owner == null) {
            return "redirect:/error";
        }
        OwnerDTO ownerDTO = new OwnerDTO(owner);
        model.addAttribute(OWNER_DTO, ownerDTO);
        return "registerOwner/step2";
    }

    @PostMapping("/step2/{slug}")
    public String step2ProcessForm(@Validated(Step2RegisterOwner.class) @ModelAttribute OwnerDTO ownerDTO, BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            return "registerOwner/step2";
        }
        Owner owner = ownerService.saveStep(ownerDTO, StepRegisterOwner.STEP2);
        try {
            request.login(ownerDTO.getEmail(), ownerDTO.getPassword());
        } catch (ServletException e) {
            log.error(e.getMessage(), e.getCause());
        }
        return "redirect:/registerOwner/step3/" + owner.getSlug();
    }

    @GetMapping("/step3/{slug}")
    public String step3Form(@PathVariable String slug, Model model) {
        Owner owner = ownerService.findBySlug(slug);
        if (owner == null) {
            return "redirect:/error";
        }
        OwnerDTO ownerDTO = new OwnerDTO(owner);
        model.addAttribute(OWNER_DTO, ownerDTO);
        return "registerOwner/step3";
    }

    @PostMapping("/step3/{slug}")
    public String step3ProcessForm(@Validated(Step3RegisterOwner.class) @ModelAttribute OwnerDTO ownerDTO, BindingResult result) {
        if (result.hasErrors()) {
            return "registerOwner/step3";
        }
        ownerService.saveStep(ownerDTO, StepRegisterOwner.STEP3);
        return "redirect:/proprietaire/ma-propriete";
    }
}
