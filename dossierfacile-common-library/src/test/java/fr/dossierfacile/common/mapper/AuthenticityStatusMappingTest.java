package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.AuthenticityStatus;
import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface AuthenticityStatusMappingTest {

    DocumentModel mapDocument(Document document);

    @ParameterizedTest
    @CsvSource({
            "TAX, VALID, TAX_ASSESSMENT, AUTHENTIC",
            "RESIDENCY, VALID, TAX_ASSESSMENT, UNKNOWN",
            "TAX, VALID, TAX_DECLARATION, UNKNOWN",
            "TAX, INVALID, TAX_ASSESSMENT, UNKNOWN",
    })
    default void should_map_authenticity_status(DocumentCategory documentCategory,
                                                FileAuthenticationStatus authenticationStatus,
                                                BarCodeDocumentType documentType,
                                                AuthenticityStatus expectedAuthenticityStatus) {
        Document document = Document.builder()
                .documentCategory(documentCategory)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .files(List.of(
                        File.builder()
                                .fileAnalysis(BarCodeFileAnalysis.builder()
                                        .barCodeType(BarCodeType.TWO_D_DOC)
                                        .authenticationStatus(authenticationStatus)
                                        .documentType(documentType)
                                        .build())
                                .build()
                ))
                .build();

        DocumentModel documentModel = mapDocument(document);

        assertThat(documentModel.getAuthenticityStatus()).isEqualTo(expectedAuthenticityStatus);
    }

}
