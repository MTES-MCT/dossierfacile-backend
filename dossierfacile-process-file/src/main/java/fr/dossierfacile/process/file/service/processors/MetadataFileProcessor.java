package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.FileMetadata;
import fr.dossierfacile.common.repository.FileMetadataRepository;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class MetadataFileProcessor implements Processor {

    public static final String XMP_CREATOR_TOOL = "xmp:CreatorTool";

    private final StorageFileLoaderService storageFileLoaderService;
    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public File process(File dfFile) {
        log.info("Reading metadata for file id: {}", dfFile.getId());
        java.io.File file = storageFileLoaderService.getTemporaryFilePath(dfFile.getStorageFile());
        try {
            var metadataMap = extractMetadata(file);
            log.info("Extracted metadata for file id: {}", dfFile.getId());
            var metadataFile = FileMetadata.builder()
                    .file(dfFile)
                    .metadata(metadataMap)
                    .build();
            fileMetadataRepository.save(metadataFile);
        } catch (Exception e) {
            log.error("Error processing metadata for file id: {}", dfFile.getId(), e);
        } finally {
            storageFileLoaderService.removeFileIfExist(file);
        }

        return dfFile;
    }

    private Map<String, String> extractMetadata(java.io.File file) {
        Map<String, String> keysOfInterest = new HashMap<>();

        // --- COMMUN & PDF ---
        keysOfInterest.put("pdf:producer", "Logiciel PDF");       // Ex: iText, Adobe, Quartz
        keysOfInterest.put(XMP_CREATOR_TOOL, "Outil de création"); // Ex: Adobe Photoshop, Microsoft Word
        keysOfInterest.put("dc:creator", "Auteur");
        keysOfInterest.put("dc:title", "Titre");
        keysOfInterest.put("dcterms:created", "Date création (Méta)");
        keysOfInterest.put("dcterms:modified", "Date modification (Méta)");

        // --- SPÉCIFIQUE IMAGES (JPG/PNG) ---
        // Le logiciel qui a édité ou sauvé l'image (Super important pour PNG/JPG)
        keysOfInterest.put("Software", "Logiciel Image");
        keysOfInterest.put("tiff:Software", "Logiciel (TIFF/Exif)");

        // Infos Matériel (Si c'est une vraie photo de carte d'identité)
        keysOfInterest.put("tiff:Make", "Marque Appareil");       // Ex: Apple, Samsung
        keysOfInterest.put("tiff:Model", "Modèle Appareil");      // Ex: iPhone 12, Galaxy S21

        // La date PRÉCISE de la prise de vue (Difficile à falsifier sans supprimer les EXIF)
        keysOfInterest.put("Exif SubIFD:Date/Time Original", "Date prise de vue");

        // Dimensions (Parfois utile pour repérer des recadrages bizarres)
        keysOfInterest.put("tiff:ImageWidth", "Largeur (px)");
        keysOfInterest.put("tiff:ImageLength", "Hauteur (px)");

        var finalMetadataMap = new LinkedHashMap<String, String>(); // LinkedHashMap pour garder l'ordre d'insertion

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (java.io.InputStream is = new java.io.FileInputStream(file)) {
            parser.parse(is, handler, metadata, context);

            for (Map.Entry<String, String> entry : keysOfInterest.entrySet()) {
                String tikaKey = entry.getKey();
                String displayLabel = entry.getValue();

                String value = metadata.get(tikaKey);

                if (value != null && !value.isBlank()) {
                    finalMetadataMap.put(displayLabel, value);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting metadata from file: {}", file.getName(), e);
        }

        return finalMetadataMap;
    }
}
