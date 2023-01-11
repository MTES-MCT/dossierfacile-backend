package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.utils.FileUtility;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class DocumentHelperServiceImpl implements DocumentHelperService {
    private final FileStorageService fileStorageService;
    private final SharedFileRepository fileRepository;
    private final EncryptionKeyService encryptionKeyService;

    @Override
    public File addFile(MultipartFile multipartFile, Document document) {
        EncryptionKey encryptionKey = encryptionKeyService.getCurrentKey();
        String path = fileStorageService.uploadFile(multipartFile, encryptionKey);


        File file = File.builder()
                .path(path)
                .document(document)
                .originalName(multipartFile.getOriginalFilename())
                .size(multipartFile.getSize())
                .contentType(multipartFile.getContentType())
                .key(encryptionKey)
                .numberOfPages(FileUtility.countNumberOfPagesOfPdfDocument(multipartFile))
                .build();
        try {
            String preview = generatePreview(multipartFile);
            file.setPreview(preview);
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            Sentry.captureException(e);
        }
        file = fileRepository.save(file);
        document.getFiles().add(file);
        return file;
    }

    @Override
    public void deleteFiles(Document document) {
        if (document.getFiles() != null && !document.getFiles().isEmpty()) {
            document.setFiles(null);
            fileRepository.deleteAll(document.getFiles());
            fileStorageService.delete(document.getFiles().stream().map(File::getPath).collect(Collectors.toList()));
        }
    }

    @Override
    public String addByteArrayToS3(byte[] file, String extension) {
        EncryptionKey encryptionKey = encryptionKeyService.getCurrentKey();
        return fileStorageService.uploadByteArray(file, extension, encryptionKey);
    }

    private String generatePreview(MultipartFile multipartFile) {
        String imageExtension = Objects.requireNonNull(multipartFile.getOriginalFilename()).substring(multipartFile.getOriginalFilename().lastIndexOf(".") + 1);
        byte[] compressImage;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage preview;
        if ("pdf".equals(imageExtension)) {
            return "";
            // TODO : fix out of memory error
//            try (PDDocument document = PDDocument.load(multipartFile.getInputStream())) {
//                PDFRenderer pdfRenderer = new PDFRenderer(document);
//                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, 600, ImageType.RGB);
//                preview = resizeImage(bufferedImage);
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//                return "";
//            }
        } else {
            try {
                preview = resizeImage(ImageIO.read(multipartFile.getInputStream()));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return "";
            }
        }
        if (preview == null) {
            return "";
        }
        try {
            ImageIO.write(preview, "jpg", baos);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return "";
        }
        compressImage = baos.toByteArray();
        return addByteArrayToS3(compressImage, imageExtension);
    }

    BufferedImage resizeImage(BufferedImage image) throws IOException {
        if (image == null) {
            return null;
        }
        float originalWidth = image.getWidth();
        float originalHeight = image.getHeight();
        int targetWidth = 300;
        int targetHeight = targetWidth < originalWidth ? (int)(targetWidth / originalWidth * originalHeight) : 300;
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

}
