package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.util.*;

@Service
public class DocumentIAResultSanitizer {
    private static final String BASE_PACKAGE = "fr.dossierfacile.document.analysis";

    private List<Class<?>> documentIaModelClasses = List.of();
    @Setter
    private List<Class<?>> documentIaModelClassesOverride;

    @PostConstruct
    public void init() {
        if (documentIaModelClassesOverride != null && !documentIaModelClassesOverride.isEmpty()) {
            this.documentIaModelClasses = List.copyOf(documentIaModelClassesOverride);
            return;
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DocumentIAModel.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(BASE_PACKAGE);
        List<Class<?>> classes = new ArrayList<>(candidates.size());
        for (BeanDefinition candidate : candidates) {
            String className = candidate.getBeanClassName();
            if (className == null) {
                continue;
            }
            try {
                classes.add(ClassUtils.forName(className, getClass().getClassLoader()));
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Impossible de charger la classe " + className, ex);
            }
        }

        this.documentIaModelClasses = Collections.unmodifiableList(classes);
    }

    // Private package for test
    List<Class<?>> getFilteredListOfClass(Document document) {
        return documentIaModelClasses.stream()
                .filter(modelClass -> {
                    DocumentIAModel annotation = modelClass.getAnnotation(DocumentIAModel.class);
                    if (annotation == null) {
                        return false;
                    }

                    DocumentCategoryStep step = annotation.documentCategoryStep();
                    DocumentSubCategory subCategory = annotation.documentSubCategory();

                    if (step != DocumentCategoryStep.UNDEFINED) {
                        return step == document.getDocumentCategoryStep();
                    }

                    if (subCategory != DocumentSubCategory.UNDEFINED) {
                        return subCategory == document.getDocumentSubCategory();
                    }

                    return annotation.documentCategory() == document.getDocumentCategory();
                })
                .toList();
    }

    public ResultModel sanitize(ResultModel resultModel, Document document) {
        if (resultModel == null
                || resultModel.getExtraction() == null
                || resultModel.getExtraction().getProperties() == null
                || document == null
                || document.getDocumentCategory() == null) {
            return resultModel;
        }

        var matchingClasses = getFilteredListOfClass(document);
        if (matchingClasses.isEmpty()) {
            return resultModel;
        }

        AllowedNames allowedNames = collectAllowedNames(matchingClasses);
        if (allowedNames.extractionNames().isEmpty()) {
            return resultModel;
        }

        filterExtractionProperties(resultModel, allowedNames.extractionNames());
        sanitizeBarcodes(resultModel, allowedNames.twoDDocNames());
        return resultModel;
    }

    private AllowedNames collectAllowedNames(List<Class<?>> matchingClasses) {
        Set<String> allowedExtractionNames = new HashSet<>();
        Set<String> allowedTwoDDocNames = new HashSet<>();
        for (Class<?> modelClass : matchingClasses) {
            for (Field field : modelClass.getDeclaredFields()) {
                DocumentIAField fieldAnnotation = field.getAnnotation(DocumentIAField.class);
                if (fieldAnnotation == null) {
                    continue;
                }
                String extractionName = fieldAnnotation.extractionName();
                if (extractionName != null && !extractionName.isBlank()) {
                    allowedExtractionNames.add(extractionName);
                }
                String twoDDocName = fieldAnnotation.twoDDocName();
                if (twoDDocName != null && !twoDDocName.isBlank()) {
                    allowedTwoDDocNames.add(twoDDocName);
                }
            }
        }
        return new AllowedNames(allowedExtractionNames, allowedTwoDDocNames);
    }

    private void filterExtractionProperties(ResultModel resultModel, Set<String> allowedExtractionNames) {
        List<GenericProperty> filteredProperties = resultModel.getExtraction().getProperties().stream()
                .filter(property -> property != null && allowedExtractionNames.contains(property.getName()))
                .toList();
        resultModel.getExtraction().setProperties(filteredProperties);
    }

    private void sanitizeBarcodes(ResultModel resultModel, Set<String> allowedTwoDDocNames) {
        if (allowedTwoDDocNames.isEmpty() || resultModel.getBarcodes() == null) {
            return;
        }
        for (var barcode : resultModel.getBarcodes()) {
            if (barcode == null) {
                continue;
            }
            barcode.setRawData(null);
            if (barcode.getTypedData() != null) {
                List<GenericProperty> filteredTypedData = barcode.getTypedData().stream()
                        .filter(property -> property != null && allowedTwoDDocNames.contains(property.getName()))
                        .toList();
                barcode.setTypedData(filteredTypedData);
            }
        }
    }

    private record AllowedNames(Set<String> extractionNames, Set<String> twoDDocNames) {
    }

}
