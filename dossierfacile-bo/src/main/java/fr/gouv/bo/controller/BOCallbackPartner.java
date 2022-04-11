package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.UserApi;
import fr.gouv.bo.model.CallbackForm;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class BOCallbackPartner {
    private static final String BO_CALLBACK_FORM = "bo/callback-form";

    private final UserApiService userApiService;

    @GetMapping("/bo/callback")
    public String formSendCallback(Model model) {
        CallbackForm callbackForm = new CallbackForm();
        List<UserApi> sources = userApiService.findAllLightApi();
        model.addAttribute("sources", sources);
        model.addAttribute("callbackForm", callbackForm);
        return BO_CALLBACK_FORM;
    }
}
