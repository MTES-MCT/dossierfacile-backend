package fr.dossierfacile.process.file.fileAnalysis.residencyTenant;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.fileAnalysis.DocumentData;
import fr.dossierfacile.fileAnalysis.TestOvhFileStorageServiceImpl;
import fr.dossierfacile.process.file.service.parsers.RentalReceipt3FParser;
import fr.dossierfacile.process.file.service.parsers.RentalReceiptParser;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.ExecutionException;

public class Helper {

    public static RentalReceiptFile parseDocument(
            DocumentData<FileDescription> documentData,
            File fileDto,
            TestOvhFileStorageServiceImpl ovhFileStorageService,
            RentalReceiptParser genericRentalParser,
            RentalReceipt3FParser rental3fParser
    ) {
        java.io.File tmpFile;
        try {
            tmpFile = ovhFileStorageService.downloadAsync(documentData.getBucketPath()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        RentalReceiptFile genericData = null;
        RentalReceiptFile rental3fData = null;
        if (genericRentalParser.shouldTryToApply(fileDto)) {
            genericData = genericRentalParser.parse(tmpFile);
        }
        if (rental3fParser.shouldTryToApply(fileDto)) {
            rental3fData = rental3fParser.parse(tmpFile);
        }
        tmpFile.deleteOnExit();
        return genericData != null ? genericData : rental3fData;
    }

    public record TenantInformation (
            String tenantFirstName,
            String tenantLastName
    ){}

    public record FileDescription (
            @JsonFormat(pattern = "yyyy-MM")
            YearMonth period,
            String tenantFullName,
            Double amount,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate paymentDate,
            Integer numberOfPage
    ){}

}
