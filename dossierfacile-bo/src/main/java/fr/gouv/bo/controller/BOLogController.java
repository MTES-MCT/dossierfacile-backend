package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Log;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.dto.Pager;
import fr.gouv.bo.service.LogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping(value = "/bo/log")
public class BOLogController {

    private static final int BUTTONS_TO_SHOW = 5;
    private static final int INITIAL_PAGE = 1;
    private static final int INITIAL_PAGE_SIZE = 50;
    private static final int[] PAGE_SIZES = {50, 100};
    private static final String EMAIL = "email";

    @Autowired
    private LogService logService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("")
    public String index(Model model, @RequestParam("pageSize") Optional<Integer> pageSize,
                        @RequestParam("page") Optional<Integer> page, @RequestParam("tenantId") Optional<String> tenantId) {

        PageRequest pageable = PageRequest.of(page.orElse(INITIAL_PAGE) - 1,
                pageSize.orElse(INITIAL_PAGE_SIZE));

        Page<Log> logs;
        if (tenantId.isPresent() && StringUtils.isNotBlank(tenantId.get())) {
            logs = logService.findAllByTenantIdPageable(
                    tenantId.map(s -> Long.valueOf(s))
                            .orElseThrow(() -> new IllegalArgumentException("Tenant must be a number")),
                    pageable);
        } else {
            logs = logService.findAllPageable(pageable);
        }

        Pager pager = new Pager(logs.getTotalPages(), logs.getNumber(), BUTTONS_TO_SHOW);

        model.addAttribute("tenantId", tenantId.orElse(""));
        model.addAttribute("logs", logs);
        model.addAttribute("pageSize", pageable.getPageSize());
        model.addAttribute("selectedPageSize", pageable.getPageSize());
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("pager", pager);

        model.addAttribute(EMAIL, new EmailDTO());
        return "bo/log";
    }
}
