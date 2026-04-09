package fr.gouv.bo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.dossierfacile.common.entity.Owner;
import fr.gouv.bo.mapper.OwnerMapper;
import fr.gouv.bo.model.owner.OwnerModel;
import fr.gouv.bo.service.OwnerService;
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

@Controller
@RequestMapping(value = "/bo/owners")
public class BOOwnerController {
    private static final String INITIAL_PAGE = "1";
    private static final String MAX_PAGE_SIZE = "20";
    private static final int[] PAGE_SIZES = {20};
    private static final int MAX_PAGE_NUMBER = 5;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private OwnerMapper ownerMapper;

    @GetMapping("")
    public String index(Model model,
                        @RequestParam(value = "page", defaultValue = INITIAL_PAGE) int page,
                        @RequestParam(value = "ownerEmail", defaultValue = "") String email,
                        @RequestParam(value = "ownerFirstname", defaultValue = "") String firstName,
                        @RequestParam(value = "ownerLastname", defaultValue = "") String lastName) {

        int pageSize = Integer.parseInt(MAX_PAGE_SIZE);
        int boundedPage = Math.clamp(page, 1, MAX_PAGE_NUMBER);
        PageRequest pageable = PageRequest.of(boundedPage - 1, pageSize, Sort.by("creationDateTime").descending());

        boolean hasSearchCriteria = !email.isBlank() || !firstName.isBlank() || !lastName.isBlank();
        Page<Owner> owners = hasSearchCriteria
                ? ownerService.searchOwners(email, firstName, lastName, pageable)
                : Page.empty(pageable);

        model.addAttribute("ownerEmail", email);
        model.addAttribute("ownerFirstname", firstName);
        model.addAttribute("ownerLastname", lastName);
        model.addAttribute("owners", owners);
        model.addAttribute("hasSearchCriteria", hasSearchCriteria);
        model.addAttribute("pageSize", pageable.getPageSize());
        model.addAttribute("pageSizes", PAGE_SIZES);

        return "bo/owners";
    }

    @GetMapping("/{id}")
    public String get(Model model, @PathVariable("id") Long id) throws JsonProcessingException {
        Owner owner = ownerService.findById(id).get();
        OwnerModel ownerModel = ownerMapper.toOwnerModel(owner);

        model.addAttribute("owner", ownerModel);
        return "bo/owner";
    }
}
