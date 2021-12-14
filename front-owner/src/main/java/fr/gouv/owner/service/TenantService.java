package fr.gouv.owner.service;


import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.owner.dto.CountDTO;
import fr.gouv.owner.dto.TenantDTO;
import fr.gouv.owner.model.KeyStatistics;
import fr.gouv.owner.projection.TenantPrincipalDTO;
import fr.gouv.owner.repository.TenantRepository;
import fr.gouv.owner.utils.UtilsLocatio;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;

@Service
@Slf4j
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ApartmentSharingService apartmentSharingService;

    public List<Tenant> getAllTenantByApartmentSharing(PropertyApartmentSharing propertyApartmentSharing){
        return tenantRepository.findTenantByApartmentSharingId(propertyApartmentSharing.getApartmentSharing().getId());
    }

    private static List<String> parseLines(String text, float width, PDFont font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = fontSize * font.getStringWidth(subString) / 1000;
            if (size > width) {
                if (lastSpace < 0) {
                    lastSpace = spaceIndex;
                }
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }
        return lines;
    }

    public ByteArrayOutputStream mergeFiles(TenantDTO tenantDTO) {
        MultipartFile[][] filesData = tenantDTO.getFiles();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (filesData != null && checkNotEmptyFiles(filesData[0])) {
            PDFMergerUtility ut = new PDFMergerUtility();
            try {
                for (MultipartFile aMultipartFile : filesData[0]) {
                    String originalName = aMultipartFile.getOriginalFilename();
                    if (originalName != null && originalName.length() > 0) {
                        String ext = FilenameUtils.getExtension(originalName);
                        if ("pdf".equalsIgnoreCase(ext)) {
                            ut.addSource(aMultipartFile.getInputStream());
                        } else {
                            ut.addSource(new ByteArrayInputStream(UtilsLocatio.convertImgToPDF(aMultipartFile)));
                        }
                    }
                }
                ut.setDestinationStream(outputStream);
                ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
            } catch (IOException e) {
                log.error(e.getMessage(), e.getCause());
                log.error("problem with validation of number of pages");
            }
        }
        return outputStream;
    }

    public boolean checkSize(ByteArrayOutputStream outputStream, double size) {
        double length = outputStream.size() / (1024.0 * 1024.0);
        return length <= size;
    }

    public boolean checkNumberOfPages(ByteArrayOutputStream outputStream, int fileNumber) {
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (PDDocument pdDocument = PDDocument.load(inputStream)) {
            if (fileNumber == 1) {
                return pdDocument.getNumberOfPages() <= 3;
            }
            if (fileNumber == 2) {
                return pdDocument.getNumberOfPages() <= 4;
            }
            if (fileNumber == 4) {
                return pdDocument.getNumberOfPages() <= 4;
            }
            if (fileNumber == 5) {
                return pdDocument.getNumberOfPages() <= 10;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e.getCause());
            log.error("problem with validation of number of pages");
            return false;
        }

        return true;
    }

    private boolean checkNotEmptyFiles(MultipartFile[] files) {
        if (files != null) {
            for (MultipartFile file : files) {
                if (!Objects.equals(file.getOriginalFilename(), "")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateTokenApartmentSharing(Tenant tenant) {

        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (apartmentSharing.getToken() == null) {
            apartmentSharing.setToken(UtilsLocatio.generateRandomString(8));
            apartmentSharing.setTokenPublic(UtilsLocatio.generateRandomString(8));
        }
        apartmentSharingService.save(apartmentSharing);
    }

    public Tenant find(int id) {
        return tenantRepository.findOneById(id);
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    /**
     * @param principal is used for AOP
     */
    public void deleteTenant(Tenant tenant, Principal principal) {
        if (null != tenant) {

            tenant.setApartmentSharing(null);
            tenantRepository.save(tenant);
            tenantRepository.delete(tenant);
        }
    }

    public void deleteJoinTenant(Tenant tenant) {
        if (null != tenant) {
            tenant.setApartmentSharing(null);
            tenantRepository.delete(tenant);

        }
    }

    public Map<KeyStatistics, Map<String, Long>> acountCreationStatistics() {
        Map<KeyStatistics, Map<String, Long>> map = new HashMap<>();
        List<CountDTO> count = tenantRepository.countAllRegisteredTenant();
        UtilsLocatio.extractStatistics(map, count, "creation");
        return map;
    }

    public Map<KeyStatistics, Map<String, Long>> boStatistics() {
        Map<KeyStatistics, Map<String, Long>> map = new HashMap<>();
        List<CountDTO> count = tenantRepository.countByUpload1IsNotNull();
        UtilsLocatio.extractStatistics(map, count, "file1");
        count = tenantRepository.countByUpload2IsNotNull();
        UtilsLocatio.extractStatistics(map, count, "file2");
        count = tenantRepository.countByUpload3IsNotNull();
        UtilsLocatio.extractStatistics(map, count, "file3");
        count = tenantRepository.countByUpload4IsNotNull();
        UtilsLocatio.extractStatistics(map, count, "file4");
        count = tenantRepository.countByUpload5IsNotNull();
        UtilsLocatio.extractStatistics(map, count, "file5");
        count = tenantRepository.countByFilesNotUploaded();
        UtilsLocatio.extractStatistics(map, count, "notUpload");
        count = tenantRepository.countByCreatedTenant();
        UtilsLocatio.extractStatistics(map, count, "tenant");

        return map;
    }

    public Map<KeyStatistics, Map<String, Long>> statistics() {
        Map<KeyStatistics, Map<String, Long>> map = new HashMap<>();
        List<CountDTO> count = tenantRepository.countByUpload1IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file1");
        count = tenantRepository.countByUpload2IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file2");
        count = tenantRepository.countByUpload3IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file3");
        count = tenantRepository.countByUpload4IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file4");
        count = tenantRepository.countByUpload5IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file5");
        count = tenantRepository.countByFilesNotUploadedTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "notUpload");

        return map;
    }

    public double countOverallSatisfaction() {
        List<Object> objects = tenantRepository.tenantsSatisfactionStatistics();
        if (objects == null || objects.isEmpty()) {
            return 0;
        }
        double satisfaction = 0;
        double insatisfaction = 0;
        for (Object object : objects) {
            if (((Object[]) object)[1] != null && (Integer) ((Object[]) object)[1] == 1) {
                satisfaction++;
            } else if (((Object[]) object)[1] != null && (Integer) ((Object[]) object)[1] == -1) {
                insatisfaction++;
            }
        }
        return (satisfaction + insatisfaction > 0) ? ((long) satisfaction / (satisfaction + insatisfaction)) : 0;
    }


    private void createText2(PDType0Font font, PDRectangle mediaBox, PDPageContentStream contentStream, String title1, String title2) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(positionX(mediaBox, title1, font), positionY(mediaBox, font, 400));
        contentStream.showText(title1);
        contentStream.endText();
        contentStream.beginText();
        contentStream.newLineAtOffset(positionX(mediaBox, title2, font), positionY(mediaBox, font, 430));
        contentStream.showText(title2);
        contentStream.endText();
    }

    private void createText(PDType0Font font, PDRectangle mediaBox, PDPageContentStream contentStream, String title1, String title2, String title3) throws IOException {
        createText2(font, mediaBox, contentStream, title1, title2);
        contentStream.beginText();
        contentStream.newLineAtOffset(positionX(mediaBox, title3, font), positionY(mediaBox, font, 460));
        contentStream.showText(title3);
        contentStream.endText();
    }

    private float positionY(PDRectangle mediaBox, PDType0Font font, int marginTop) {
        int fontSize = 20;
        float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        return mediaBox.getHeight() - marginTop - titleHeight;
    }

    private float positionX(PDRectangle mediaBox, String title, PDType0Font font) throws IOException {
        int fontSize = 20;
        float titleWidth = font.getStringWidth(title) / 1000 * fontSize;
        return (mediaBox.getWidth() - titleWidth) / 2;
    }

    public Long countUploadedFiles() {
        return tenantRepository.countTotalUploadedFiles();
    }

    public Tenant findOneByEmail(String email) {
        return tenantRepository.findOneByEmail(email);
    }

    public void setSurveyResponse(Tenant tenant, int response) {
        tenant.setSatisfactionSurvey(response);
        tenantRepository.save(tenant);
    }

    public void save(Tenant tenant, TenantDTO tenantDTO) {
        tenant.setFirstName(tenantDTO.getFirstName());
        tenant.setLastName(tenantDTO.getLastName());
        tenantRepository.save(tenant);
    }

    public void editJoinTenant(Tenant join, TenantDTO tenantDTO) {
        join.setFirstName(tenantDTO.getFirstName());
        join.setLastName(tenantDTO.getLastName());
        tenantRepository.save(join);
    }

    public void setNullApartmentSharing() {
        tenantRepository.deletedTenantsSetNullApartmentSharing();
    }

    public TenantPrincipalDTO findPrincipalByApartmentSharing(int id) {
        return tenantRepository.findPrincipalTenant(id);
    }

    public List<Tenant> findAllByApartmentSharing(int id) {
        return tenantRepository.findAllByApartmentSharingId(id);
    }

    public Tenant findOneByApartmentSharingIdAndTenantType(int id, TenantType type) {
        return tenantRepository.findOneByApartmentSharingIdAndTenantType(id, type);
    }
}


