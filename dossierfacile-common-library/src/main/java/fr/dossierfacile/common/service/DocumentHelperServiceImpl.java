package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.utils.FileUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        File file = fileRepository.save(
                File.builder()
                        .path(path)
                        .document(document)
                        .originalName(multipartFile.getOriginalFilename())
                        .size(multipartFile.getSize())
                        .contentType(multipartFile.getContentType())
                        .key(encryptionKey)
                        .numberOfPages(FileUtility.countNumberOfPagesOfPdfDocument(multipartFile))
                        .build()
        );
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
}
