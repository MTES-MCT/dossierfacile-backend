package fr.dossierfacile.api.front.util;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class FilePageCounter {

    private final List<MultipartFile> files;

    public int getTotalNumberOfPages() throws IOException {
        int nonPdfFilesCount = 0;
        List<MultipartFile> pdfFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            if ("application/pdf".equals(file.getContentType())) {
                pdfFiles.add(file);
            } else {
                nonPdfFilesCount++;
            }
        }

        int pdfTotalPages = mergePdfFiles(pdfFiles)
                .map(PDDocument::getNumberOfPages)
                .orElse(0);

        return nonPdfFilesCount + pdfTotalPages;
    }

    private Optional<PDDocument> mergePdfFiles(List<MultipartFile> pdfFiles) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        pdfMerger.setDestinationStream(outputStream);

        for (MultipartFile pdfFile : pdfFiles) {
            pdfMerger.addSource(new RandomAccessReadBuffer(pdfFile.getInputStream()));
        }
        pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly().streamCache);

        if (outputStream.size() <= 0) {
            return Optional.empty();
        }

        try (PDDocument document = Loader.loadPDF(outputStream.toByteArray())) {
            return Optional.of(document);
        }
    }

}
