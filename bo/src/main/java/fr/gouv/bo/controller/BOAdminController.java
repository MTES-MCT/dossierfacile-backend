package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.User;
import fr.gouv.bo.dto.UserDTO;
import fr.gouv.bo.service.UserRoleService;
import fr.gouv.bo.service.UserService;
import fr.gouv.bo.validator.interfaces.CreateUser;
import fr.gouv.bo.validator.interfaces.UpdateUser;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/bo/admin")
public class BOAdminController {

    private static final String REDIRECT_BO_ADMIN = "redirect:/bo/admin";

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ModelMapper modelMapper;

    @GetMapping("")
    public String index(Model model) {
        List<User> adminList = userService.findAllAdmins();
        model.addAttribute("adminList", adminList);
        return "bo/admin";
    }

    @GetMapping("/new")
    public String create(Model model) {
        UserDTO userDTO = new UserDTO();
        model.addAttribute("userDTO", userDTO);
        model.addAttribute("new", true);
        return "bo/admin-form";
    }

    @PostMapping("")
    public String create(@Validated(CreateUser.class) UserDTO userDTO, BindingResult result) {
        if (result.hasErrors()) {
            return "bo/admin-form";
        }
        User user = userService.save(userDTO);
        userRoleService.save(user, userDTO);
        return REDIRECT_BO_ADMIN;
    }

    @GetMapping("/{id}")
    public String edit(Model model, @PathVariable Long id) {
        User user = userService.findOne(id);
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        model.addAttribute("userDTO", userDTO);
        return "bo/admin-form-edit";
    }

    @PostMapping("/{id}")
    public String edit(@Validated(UpdateUser.class) UserDTO userDTO, BindingResult result) {
        if (result.hasErrors()) {
            return "bo/admin-form-edit";
        }
        userService.update(userDTO);
        return REDIRECT_BO_ADMIN;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        userService.delete(id);
        return REDIRECT_BO_ADMIN;
    }
}
