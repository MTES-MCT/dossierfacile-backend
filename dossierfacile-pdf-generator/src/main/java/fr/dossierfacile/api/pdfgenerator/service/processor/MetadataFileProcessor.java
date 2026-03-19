package fr.dossierfacile.api.pdfgenerator.service.processor;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.FileMetadata;
import fr.dossierfacile.common.repository.FileMetadataRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class MetadataFileProcessor {

    public static final String XMP_CREATOR_TOOL = "xmp:CreatorTool";

    private final FileMetadataRepository fileMetadataRepository;

    public File process(InputStream inputStream, File dfFile) {
        log.info("Reading metadata for file id: {}", dfFile.getId());
        try {
            var metadataMap = extractMetadata(inputStream, dfFile);
            log.info("Extracted metadata for file id: {}", dfFile.getId());
            var metadataFile = FileMetadata.builder()
                    .file(dfFile)
                    .metadata(metadataMap)
                    .build();
            fileMetadataRepository.save(metadataFile);
        } catch (Exception e) {
            log.error("Error processing metadata for file id: {}", dfFile.getId(), e);
        }

        return dfFile;
    }

    private Map<String, String> extractMetadata(InputStream inputStream, File file) {
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
        // Handler qui ignore le contenu extrait
        ContentHandler handler = new DefaultHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setExtractUniqueInlineImagesOnly(false);
        // This is applied only for PDF
        pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);

        context.set(PDFParserConfig.class, pdfConfig);

        // This will also apply the ocr skip for all other mime types.
        // jpg / png / ...
        TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
        ocrConfig.setSkipOcr(true);
        context.set(TesseractOCRConfig.class, ocrConfig);

        try {
            parser.parse(inputStream, handler, metadata, context);

            for (Map.Entry<String, String> entry : keysOfInterest.entrySet()) {
                String tikaKey = entry.getKey();
                String displayLabel = entry.getValue();

                String value = metadata.get(tikaKey);

                if (value != null && !value.isBlank()) {
                    finalMetadataMap.put(displayLabel, value);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting metadata from file: {}", file.getId(), e);
        }

        return finalMetadataMap;
    }
}