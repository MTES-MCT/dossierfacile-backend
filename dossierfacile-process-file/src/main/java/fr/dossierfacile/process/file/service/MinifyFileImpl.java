package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import fr.dossierfacile.process.file.service.interfaces.MinifyFile;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinifyFileImpl implements MinifyFile {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentHelperService documentHelperService;

    @Override
    public void process(Long fileId) {
        fileRepository.findById(fileId)
                .ifPresent(file -> {
                    try (InputStream inputStream = fileStorageService.download(file)) {
                        String preview = documentHelperService.generatePreview(inputStream, file.getOriginalName());
                        file.setPreview(preview);
                        fileRepository.save(file);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e.getCause());
                        Sentry.captureException(e);
                    }
                });
    }

}
