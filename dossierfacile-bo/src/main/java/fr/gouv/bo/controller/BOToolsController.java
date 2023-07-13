package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.service.ToolsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class BOToolsController {

    private final FileStorageService fileStorageService;
    private final StorageFileRepository storageFileRepository;

    private final ToolsService toolsService;

    @GetMapping("/bo/tools")
    public String outils(Model model) {
        model.addAttribute("email", new EmailDTO());
        model.addAttribute("files", toolsService.getFiles());
        return "bo/tools";
    }

    @PostMapping("/bo/tools/lamanufacture")
    public String lamanufacture(Model model, @RequestParam("count") int count) {
        try {
            toolsService.generateLastTreatedDossiers(count);
        } catch (Exception e) {
            log.error("not working");
        }

        return "redirect:/bo/tools";
    }

    @GetMapping(value = "/bo/tools/lamanufacture/files/{fileId}")
    public void getLaManufactureFiles(HttpServletResponse response, @PathVariable("fileId") Long fileId) {
        // Get zip from storage Id
        StorageFile file = storageFileRepository.findById(fileId).get();

        try (InputStream in = fileStorageService.download(file)) {
            response.setContentType(file.getContentType());
            IOUtils.copy(in, response.getOutputStream());
        } catch (final IOException e) {
            log.error("Error", e);
            response.setStatus(404);
        }
    }

}