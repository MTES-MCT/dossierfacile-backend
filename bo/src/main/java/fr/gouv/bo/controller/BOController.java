package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import fr.dossierfacile.common.service.interfaces.OvhService;
import fr.gouv.bo.amqp.Producer;
import fr.gouv.bo.dto.DeleteUserDTO;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.dto.Pager;
import fr.gouv.bo.dto.UserDTO;
import fr.gouv.bo.model.FileForm;
import fr.gouv.bo.service.ApartmentSharingService;
import fr.gouv.bo.service.DocumentService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserRoleService;
import fr.gouv.bo.service.UserService;
import fr.gouv.bo.utils.UtilsLocatio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BOController {

    private static final int BUTTONS_TO_SHOW = 5;
    private static final int INITIAL_PAGE = 0;
    private static final int INITIAL_PAGE_SIZE = 100;
    private static final int[] PAGE_SIZES = {100, 200};
    private static final String REDIRECT_BO = "redirect:/bo";

    private final ApartmentSharingService apartmentSharingService;
    private final TenantService tenantService;
    private final UserService userService;
    private final OvhService ovhService;
    private final DocumentService documentService;
    private final Producer producer;
    private final UserRoleService userRoleService;

    @GetMapping("/")
    public String index(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            return REDIRECT_BO;
        }
    }

    @GetMapping("/callback/manually/{userApiId}")
    public String callBackManuallyToUserApi(@PathVariable("userApiId") Long userApiId) {
        tenantService.sendCallBacksManuallyToUserApi(userApiId);
        return REDIRECT_BO;
    }

    @GetMapping("/callback/manually/locserviceids")
    public String callBackManuallyToLocserviceIds() {
        tenantService.callBackManuallyToLocserviceIds();
        return REDIRECT_BO;
    }

    @GetMapping("/computeStatusOfAllTenants")
    public String computeStatusOfAllTenants() {
        tenantService.computeStatusOfAllTenants();
        return REDIRECT_BO;
    }

    @GetMapping("/bo")
    public String bo(Model model, @RequestParam("pageSize") Optional<Integer> pageSize, @RequestParam("page") Optional<Integer> page, Principal principal) {
        int evalPageSize = pageSize.orElse(INITIAL_PAGE_SIZE);
        int val = 0;
        if (page.isPresent()) {
            val = page.get() - 1;
        }
        int evalPage = (page.orElse(0) < 1) ? INITIAL_PAGE : val;
        EmailDTO emailDTO = new EmailDTO();
        Page<Tenant> tenants = tenantService.listTenantsToProcess(PageRequest.of(evalPage, evalPageSize));
        Pager pager = new Pager(tenants.getTotalPages(), tenants.getNumber(), BUTTONS_TO_SHOW);
        User login_user = userService.findUserByEmail(principal.getName());
        boolean is_admin = login_user.getUserRoles().stream().anyMatch(userRole -> userRole.getRole().name().equals(Role.ROLE_ADMIN.name()));

        model.addAttribute("tenantToProcess", tenantService.getTenantsWithStatusInToProcess());
        model.addAttribute("TenantsWithFailedGeneratedPdf",tenantService.getTotalOfTenantsWithFailedGeneratedPdfDocument());
        model.addAttribute("loginUser", is_admin);
        model.addAttribute("tenants", tenants);
        model.addAttribute("selectedPageSize", evalPageSize);
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("pager", pager);
        model.addAttribute("email", emailDTO);
        return "bo/index";
    }

    @GetMapping("/bo/create/admin")
    public String getAdmin(Model model) {
        EmailDTO emailDTO1 = new EmailDTO();
        model.addAttribute("email", emailDTO1);
        return "bo/create-admin";
    }

    @PostMapping("/bo/create/admin")
    public String createOrUpdateUserToAdmin(EmailDTO emailDTO, Model model, @RequestParam(name = "action") String create_user) {
        EmailDTO emailDTO1 = new EmailDTO();
        User user = userService.findUserByEmail(emailDTO.getEmail());
        if (user != null) {
            if (user.getUserRoles().isEmpty()) {
                userRoleService.createRoleAdminByEmail(emailDTO.getEmail(), user, create_user);
            } else {
                UserRole userRole1;
                if(create_user.equals("create_admin")){
                    userRole1 = user.getUserRoles().stream().filter(userRole -> userRole.getRole().name().equals(Role.ROLE_ADMIN.name())).findFirst().orElse(null);
                } else{
                    userRole1 = user.getUserRoles().stream().filter(userRole -> userRole.getRole().name().equals(Role.ROLE_OPERATOR.name())).findFirst().orElse(null);
                }
                if (userRole1 == null) {
                    userRoleService.createRoleAdminByEmail(emailDTO.getEmail(), user, create_user);
                }
            }
        } else {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(emailDTO.getEmail());
            userService.save(userDTO);
            userRoleService.createRoleAdminByEmail(userDTO.getEmail(), null, create_user);
        }

        model.addAttribute("email", emailDTO1);
        return "bo/create-admin";
    }

    @GetMapping("/bo/searchTenant")
    public String searchTenant(Model model, EmailDTO emailDTO, RedirectAttributes redirectAttributes) {

        List<Tenant> tenantList = tenantService.getTenantByIdOrEmail(emailDTO);

        if (tenantList.isEmpty() || emailDTO.getEmail().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "No tenant by that name !");
            redirectAttributes.addFlashAttribute("alertClass", "alert-danger");
            return REDIRECT_BO;
        }

        if (tenantList.get(0) == null) {
            redirectAttributes.addFlashAttribute("message", "Tenant not found !");
            redirectAttributes.addFlashAttribute("alertClass", "alert-danger");
            return REDIRECT_BO;
        }

        if (emailDTO.getEmail().contains("@") || UtilsLocatio.isNumeric(emailDTO.getEmail())) {
            return "redirect:/bo/colocation/" + tenantList.get(0).getApartmentSharing().getId();
        }

        EmailDTO emailDTOSave = new EmailDTO();
        model.addAttribute("email", emailDTOSave);
        model.addAttribute("matchList", tenantList);
        model.addAttribute("keySearch", emailDTO.getEmail());

        return "bo/search";

    }

    @GetMapping("/bo/searchResult")
    public String searchResult(Model model, @RequestParam(value = "q", defaultValue = "") String q,
                               @RequestParam("pageSize") Optional<Integer> pageSize, @RequestParam("page") Optional<Integer> page) {
        int evalPageSize = pageSize.orElse(INITIAL_PAGE_SIZE);
        int val = 0;
        if (page.isPresent()) {
            val = page.get() - 1;
        }
        int evalPage = (page.orElse(0) < 1) ? INITIAL_PAGE : val;
        Page<Tenant> tenants = tenantService.listTenantsFilter(PageRequest.of(evalPage, evalPageSize), q);
        Pager pager = new Pager(tenants.getTotalPages(), tenants.getNumber(), BUTTONS_TO_SHOW);
        model.addAttribute("tenants", tenants);
        model.addAttribute("selectedPageSize", evalPageSize);
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("pager", pager);
        return "bo/searchResult";
    }

    @GetMapping("/bo/nextApplication")
    public String nextApplication(Principal principal, @RequestParam(value = "tenant_id", required = false) Long tenantId) {
        if (principal == null) {
            return "redirect:/error";
        }
        return tenantService.redirectToApplication(principal, tenantId);
    }

    @GetMapping("/bo/deleteAccount")
    public String getDeleteAccount(Model model) {
        DeleteUserDTO deleteUser = new DeleteUserDTO();
        model.addAttribute("deleteUser", deleteUser);
        return "bo/delete-account";
    }

    @PostMapping("/bo/deleteAccount")
    public String postDeleteAccount(@Validated @ModelAttribute("deleteUser") DeleteUserDTO deleteUser, BindingResult result, Principal principal) {
        if (result.hasErrors()) {
            return "bo/delete-account";
        }
        User user = userService.findUserByEmail(deleteUser.getEmail());
        if (user instanceof Tenant) {
            Tenant tenant = (Tenant) user;
            tenantService.partnerCallBackServiceWhenDeleteTenant(tenant);
            apartmentSharingService.delete(tenant.getApartmentSharing());
        } else {
            if (!principal.getName().equals(deleteUser.getEmail())) {
                userService.delete(user.getId());
            }
        }
        return "redirect:/bo/deleteAccount";
    }


    @GetMapping("/bo/deleteFile")
    public String deleteFileForm(Model model) {
        FileForm fileForm = new FileForm();
        model.addAttribute("fileForm", fileForm);
        return "bo/delete-file";
    }

    @PostMapping("/bo/deleteFile")
    public String deleteFileProcess(@Valid @ModelAttribute FileForm fileForm, BindingResult result) {
        if (result.hasErrors()) {
            return "bo/deleteFile";
        }
        ovhService.deleteAllFiles(fileForm.getPath());
        return "redirect:/bo/deleteFile";
    }

    @GetMapping("/bo/regeneratePdfDocument/{id}")
    public String regeneratePdfDocument(@PathVariable Long id) {
        Document document = documentService.findDocumentById(id);
        documentService.initializeFieldsToProcessPdfGeneration(document);
        producer.generatePdf(id);
        Tenant tenant = document.getTenant() != null ? document.getTenant() : document.getGuarantor().getTenant();
        long apartmentSharingId = tenant.getApartmentSharing().getId();
        return "redirect:/bo/colocation/" + apartmentSharingId + "#tenant" + tenant.getId();
    }

    @GetMapping("/bo/regeneratePdfDocument/withMaxRetriesReached")
    public String regenerateFailedPdfDocumentsManually() {
        documentService.regenerateFailedPdfDocuments();
        return "redirect:/bo";
    }

    @Scheduled(cron = "0 1 0 * * ?")
    public void regenerateFailedPdfDocumentsTask() {
        log.info("Checking for failed PDF generation");
        documentService.regenerateFailedPdfDocuments();
    }

    @PostMapping(value = "/bo/regenerateStatusOfTenants")
    public String regenerateStatusOfTenants(@RequestParam("email") String email) {
        tenantService.updateStatusOfSomeTenants(email);
        return "redirect:/bo";
    }

    @GetMapping("/bo/computeTenantsStatus")
    public String computeTenantStatus(Model model) {
        EmailDTO emailDTO = new EmailDTO();
        model.addAttribute("tenantList", emailDTO);
        model.addAttribute("email", emailDTO);
        return "bo/compute-status";
    }

    @GetMapping("/bo/deleteAccountsNotProperlyDeleted")
    public String deleteAccountsNotProperlyDeleted() {
        tenantService.deleteAccountsNotProperlyDeleted();
        return "redirect:/bo";
    }

    @GetMapping("/update/documents/creationDate")
    public String updateDocumentsWithNullCretionDate() {
        tenantService.updateDocumentsWithNullCreationDateTime();
        return REDIRECT_BO;
    }
}
