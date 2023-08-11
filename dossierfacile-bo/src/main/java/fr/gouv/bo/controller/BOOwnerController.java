package fr.gouv.bo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.dossierfacile.common.entity.Owner;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.dto.Pager;
import fr.gouv.bo.mapper.OwnerMapper;
import fr.gouv.bo.model.owner.OwnerModel;
import fr.gouv.bo.service.OwnerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping(value = "/bo/owners")
public class BOOwnerController {

    private static final int BUTTONS_TO_SHOW = 5;
    private static final int INITIAL_PAGE = 1;
    private static final int INITIAL_PAGE_SIZE = 50;
    private static final int[] PAGE_SIZES = {50, 100};
    private static final String EMAIL = "email";

    @Autowired
    private OwnerService ownerService;
    @Autowired
    private OwnerMapper ownerMapper;

    @GetMapping("")
    public String index(Model model, @RequestParam("pageSize") Optional<Integer> pageSize,
                        @RequestParam("page") Optional<Integer> page, @RequestParam("ownerEmail") Optional<String> email) {

        PageRequest pageable = PageRequest.of(page.orElse(INITIAL_PAGE) - 1,
                pageSize.orElse(INITIAL_PAGE_SIZE), Sort.by("creationDateTime").descending());

        Page<Owner> owners;
        if (email.isPresent() && StringUtils.isNotBlank(email.get())) {
            owners = ownerService.findAllByEmailExpressionPageable(email.get(), pageable);
        } else {
            owners = ownerService.findAllPageable(pageable);
        }

        Pager pager = new Pager(owners.getTotalPages(), owners.getNumber(), BUTTONS_TO_SHOW);

        model.addAttribute("ownerEmail", email.orElse(""));
        model.addAttribute("owners", owners);
        model.addAttribute("pageSize", pageable.getPageSize());
        model.addAttribute("selectedPageSize", pageable.getPageSize());
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("pager", pager);

        model.addAttribute(EMAIL, new EmailDTO());
        return "bo/owners";
    }

    @GetMapping("/{id}")
    public String get(Model model, @PathVariable("id") Long id) throws JsonProcessingException {
        Owner owner = ownerService.findById(id).get();
        OwnerModel ownerModel = ownerMapper.toOwnerModel(owner);

        model.addAttribute("owner", ownerModel);
        model.addAttribute(EMAIL, new EmailDTO());
        return "bo/owner";
    }
}
