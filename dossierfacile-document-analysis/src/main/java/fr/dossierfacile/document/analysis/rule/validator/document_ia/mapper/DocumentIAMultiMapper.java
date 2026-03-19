package fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;

import java.util.*;

/*
This mapper is used to return a list of multiple analyses on a document.
It uses the @DocumentIAField annotation to map fields from the analyses to the target object.
It prioritizes 2DDoc data over Extraction data
 */
public class DocumentIAMultiMapper extends BaseDocumentIAMapper {

    public <T> List<T> map(List<DocumentIAFileAnalysis> documentIAAnalyses, Class<T> targetClass) {
            var listOfResults = new ArrayList<T>();

            for (DocumentIAFileAnalysis documentIAFileAnalysis : documentIAAnalyses) {

                List<GenericProperty> doc2DProperties = extract2DDocItems(documentIAFileAnalysis);
                List<GenericProperty> extractionProperties = extractExtractionItems(documentIAFileAnalysis);

                var instantiated = instantiate(
                        doc2DProperties,
                        extractionProperties,
                        targetClass
                );
                instantiated.ifPresent(listOfResults::add);
            }

            return listOfResults;
    }

    private List<GenericProperty> extract2DDocItems(DocumentIAFileAnalysis documentIAAnalyse) {
        return documentIAAnalyse
                .getResult()
                .getBarcodes()
                .stream()
                .filter(Objects::nonNull)
                .map(BarcodeModel::getTypedData)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }

    private List<GenericProperty> extractExtractionItems(DocumentIAFileAnalysis documentIAAnalyse) {
        var extraction = documentIAAnalyse.getResult().getExtraction();
        if (extraction == null) {
            return Collections.emptyList();
        }
        return documentIAAnalyse
                .getResult()
                .getExtraction()
                .getProperties();
    }
}