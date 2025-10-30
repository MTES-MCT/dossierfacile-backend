package fr.dossierfacile.common.service;

import fr.dossierfacile.common.config.ImageMagickConfig;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.utils.FileUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class DocumentHelperServiceImpl implements DocumentHelperService {
    private final FileStorageService fileStorageService;
    private final SharedFileRepository fileRepository;
    private final EncryptionKeyService encryptionKeyService;
    private final ImageMagickConfig imageMagickConfig;

    @Transactional
    @Override
    public File addFile(MultipartFile multipartFile, Document document) throws IOException {
        StorageFile storageFile = StorageFile.builder()
                .size(multipartFile.getSize())
                .encryptionKey(encryptionKeyService.getCurrentKey())
                .bucket(S3Bucket.RAW_FILE)
                .build();
        String originalFilename = multipartFile.getOriginalFilename();
        storageFile.setMd5(getFileMd5Hash(multipartFile));
        if (originalFilename == null) {
            originalFilename = UUID.randomUUID().toString();
        }
        storageFile.setPath(document.getDocumentS3PrefixPath() + "/" + UUID.randomUUID());
        if ("image/heif".equals(multipartFile.getContentType())) {
            storageFile.setName(originalFilename.replaceAll("(?i)\\.heic$", "") + ".jpg");
            storageFile.setContentType("image/jpeg");

            InputStream jpgInputStream = convertHeicToJpg(multipartFile.getInputStream());
            if (jpgInputStream != null) {
                storageFile = fileStorageService.upload(jpgInputStream, storageFile);
            } else {
                throw new IOException("Image could not be saved");
            }
        } else {
            storageFile.setName(originalFilename);
            storageFile.setContentType(multipartFile.getContentType());
            storageFile = fileStorageService.upload(multipartFile.getInputStream(), storageFile);
        }

        File file = File.builder()
                .storageFile(storageFile)
                .document(document)
                .numberOfPages(FileUtility.countNumberOfPagesOfPdfDocument(multipartFile))
                .build();
        file = fileRepository.save(file);
        if (!document.getFiles().contains(file)) {
            document.getFiles().add(file);
        }
        return file;
    }

    @Override
    public InputStream convertHeicToJpg(InputStream heicInputStream) throws IOException {
        String tmpImageName = UUID.randomUUID().toString();
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
        java.io.File heicFile = java.nio.file.Files.createTempFile(tmpImageName, ".heic", attr).toFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(heicFile)) {
            IOUtils.copy(heicInputStream, fileOutputStream);
        }

        java.io.File jpgFile = java.nio.file.Files.createTempFile(tmpImageName, ".jpg", attr).toFile();

        // Use ImageMagick to convert .heic to .jpg
        ProcessBuilder processBuilder = new ProcessBuilder(imageMagickConfig.getImageMagickCli(), heicFile.getAbsolutePath(), jpgFile.getAbsolutePath());
        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Erreur lors de la conversion HEIC en JPG avec ImageMagick.");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted exception", e);
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        InputStream jpgInputStream = new FileInputStream(jpgFile);

        boolean delete = heicFile.delete();
        if (!delete) {
            log.error("Could not delete temporary file");
        }
        jpgFile.deleteOnExit();

        return jpgInputStream;
    }

    @Override
    public void deleteFiles(Document document) {
        if (document.getFiles() != null && !document.getFiles().isEmpty()) {
            List<File> files = document.getFiles();
            document.setFiles(null);
            fileRepository.deleteAll(files);
        }
    }

    @Override
    public StorageFile generatePreview(Document document, InputStream fileInputStream, String originalName) {
        try {
            String imageExtension = FilenameUtils.getExtension(originalName);
            BufferedImage preview;
            if ("pdf".equalsIgnoreCase(imageExtension)) {
                long startTime = System.currentTimeMillis();
                try (PDDocument pdfDocument = Loader.loadPDF(fileInputStream.readAllBytes())) {
                    PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
                    BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, 200, ImageType.RGB);
                    preview = resizeImage(bufferedImage);
                    log.info("resize pdf duration : {}", System.currentTimeMillis() - startTime);
                }
            } else {
                preview = resizeImage(ImageIO.read(fileInputStream));
            }
            if (preview == null) {
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(preview, "jpg", baos);

            StorageFile storageFile = StorageFile.builder()
                    .name(originalName)
                    .bucket(S3Bucket.RAW_MINIFIED)
                    .encryptionKey(encryptionKeyService.getCurrentKey())
                    .path(document.getDocumentS3PrefixPath() + "/" + UUID.randomUUID())
                    .contentType(MediaType.IMAGE_JPEG_VALUE)
                    .encryptionKey(null)
                    .build();

            try (InputStream is = new ByteArrayInputStream(baos.toByteArray())) {
                return fileStorageService.upload(is, storageFile);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    BufferedImage resizeImage(BufferedImage image) {
        if (image == null) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        float originalWidth = image.getWidth();
        float originalHeight = image.getHeight();
        int targetWidth = 300;
        int targetHeight = targetWidth < originalWidth ? (int) (targetWidth / originalWidth * originalHeight) : 300;
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setColor(Color.WHITE);
        graphics2D.fillRect(0, 0, targetWidth, targetHeight);
        graphics2D.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        log.info("resize image duration : {}", duration);
        return resizedImage;
    }

    private String getFileMd5Hash(MultipartFile file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(file.getBytes());
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found", e);
            return null;
        }
    }
}
