package fr.dossierfacile.common.service;

import fr.dossierfacile.common.config.ThreeDSOutscaleConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {OutscaleFileStorageServiceImpl.class, ThreeDSOutscaleConfig.class})
@TestPropertySource(locations = "classpath:secret/application-test.properties")
public class OutscaleFileStorageServiceImplTest {

    @Autowired
    private OutscaleFileStorageServiceImpl storageService;

    @Test
    public void testUploadAndDownload() throws Exception {
        final String fileName = "test.txt";
        final String content = "Contenu de test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        // Test upload
        storageService.upload(fileName, inputStream, null, "text/plain");

        // Test download
        InputStream downloadedInputStream = storageService.download(fileName, null);
        String downloadedContent = new BufferedReader(new InputStreamReader(downloadedInputStream))
                .lines().collect(Collectors.joining("\n"));

        // check file content
        assertEquals(content, downloadedContent);
    }

}
