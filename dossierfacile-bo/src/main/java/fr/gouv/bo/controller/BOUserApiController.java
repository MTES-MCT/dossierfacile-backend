package fr.gouv.bo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.constants.PartnerConstants;
import fr.dossierfacile.common.entity.UserApi;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.service.UserApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/bo/userApi")
@Slf4j
public class BOUserApiController {
    private static final String REDIRECT_URL = "redirect:/bo/userApi";
    private final UserApiService userApiService;
    private final ObjectMapper mapper;

    @GetMapping("")
    public String index(Model model) {
        List<UserApi> userApiList = userApiService.findAll();
        model.addAttribute("userApiList", userApiList);
        model.addAttribute("userApiDTO", new UserApiDTO());
        return "bo/user-api";
    }

    @PostMapping("")
    public String create(@Validated @ModelAttribute("userApiDTO") UserApiDTO userApiDTO, BindingResult result) {
        rejectCompletedStatusForOwner(userApiDTO, result);
        if (result.hasErrors()) {
            log.error("BOUserApiController create has errors: {}", result.getAllErrors());
            return REDIRECT_URL;
        }
        userApiService.create(userApiDTO);
        return REDIRECT_URL;
    }

    @GetMapping("/{id}")
    public String updateForm(@PathVariable("id") Long id, Model model) {
        UserApi userApi = userApiService.findById(id);
        if (userApi == null) {
            log.error("BOUserApiController updateForm not found userApi with id : {}", id);
            return "redirect:/error";
        }
        UserApiDTO userApiDTO = mapper.convertValue(userApi, UserApiDTO.class);
        model.addAttribute("userApiDTO", userApiDTO);
        model.addAttribute("userApi", userApi);
        return "bo/user-api-edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id, @Validated @ModelAttribute("userApiDTO") UserApiDTO userApiDTO, BindingResult result) {
        keepCompletedStatusOnceIntegrated(id, userApiDTO);
        rejectCompletedStatusForOwner(userApiDTO, result);
        if (result.hasErrors()) {
            log.error("BOUserApiController update has errors: {}", result.getAllErrors());
            return "bo/user-api-edit";
        }
        userApiService.save(userApiDTO);
        return REDIRECT_URL;
    }

    // No rollback per partner: the COMPLETED integration is final. The checkbox is disabled in the
    // form once checked, and a disabled checkbox is not posted (bound to false), so the persisted
    // value must win over the form here
    private void keepCompletedStatusOnceIntegrated(Long id, UserApiDTO userApiDTO) {
        if (userApiService.findById(id).isCompletedStatusSupported()) {
            userApiDTO.setCompletedStatusSupported(true);
        }
    }

    // The owner space is excluded from the COMPLETED status (owner mappers and mails are unconditional)
    private void rejectCompletedStatusForOwner(UserApiDTO userApiDTO, BindingResult result) {
        if (userApiDTO.isCompletedStatusSupported() && PartnerConstants.DF_OWNER_NAME.equals(userApiDTO.getName())) {
            result.rejectValue("completedStatusSupported", "userApi.completedStatusSupported.owner",
                    "Le partenaire " + PartnerConstants.DF_OWNER_NAME + " (espace propriétaire) ne peut pas intégrer le statut COMPLETED.");
        }
    }

}
