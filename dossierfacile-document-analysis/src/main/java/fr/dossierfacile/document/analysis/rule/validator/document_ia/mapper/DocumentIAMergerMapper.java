package fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/*
This mapper is used to merge multiple analyses Front and Back on a document into a single object.
It uses the @DocumentIAField annotation to map fields from the analyses to the target object.
It prioritizes 2DDoc data over Extraction data and the last analysis over the first one
 */
public class DocumentIAMergerMapper extends BaseDocumentIAMapper {

    public <T> Optional<T> map(List<DocumentIAFileAnalysis> documentIAAnalyses, Class<T> targetClass) {
        var listOf2DDocItems = extract2DDocItems(documentIAAnalyses);
        var listOfExtractionItems = extractExtractionItems(documentIAAnalyses);

        return instantiate(
                listOf2DDocItems,
                listOfExtractionItems,
                targetClass
        );
    }

    private static List<GenericProperty> extract2DDocItems(List<DocumentIAFileAnalysis> documentIAAnalyses) {
        return documentIAAnalyses
                .stream()
                .map(DocumentIAFileAnalysis::getResult)
                .map(ResultModel::getBarcodes)
                .flatMap(Collection::stream)
                .map(BarcodeModel::getTypedData)
                .flatMap(Collection::stream)
                .toList()
                .reversed(); // We reverse the list to prioritize the last analysis results
    }

    private static List<GenericProperty> extractExtractionItems(List<DocumentIAFileAnalysis> documentIAAnalyses) {
        return documentIAAnalyses
                .stream()
                .map(DocumentIAFileAnalysis::getResult)
                .map(ResultModel::getExtraction)
                .filter(Objects::nonNull)
                .flatMap(it -> it.getProperties().stream())
                .toList()
                .reversed(); // We reverse the list to prioritize the last analysis results
    }
}