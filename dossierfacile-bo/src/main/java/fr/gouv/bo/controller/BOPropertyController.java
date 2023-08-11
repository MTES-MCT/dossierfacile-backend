package fr.gouv.bo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.dossierfacile.common.entity.Property;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.mapper.PropertyMapper;
import fr.gouv.bo.model.owner.PropertyModel;
import fr.gouv.bo.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/bo/properties")
public class BOPropertyController {
    private static final String EMAIL = "email";

    @Autowired
    private PropertyService propertyService;
    @Autowired
    private PropertyMapper propertyMapper;


    @GetMapping("/{id}")
    public String get(Model model, @PathVariable("id") Long id) throws JsonProcessingException {
        Property property = propertyService.findById(id).get();
        PropertyModel propertyModel = propertyMapper.toPropertyModel(property);

        model.addAttribute("property", propertyModel);
        model.addAttribute(EMAIL, new EmailDTO());
        return "bo/property";
    }
}
