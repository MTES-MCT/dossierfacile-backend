package fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.model.documentIA.BarcodeModel;
import fr.dossierfacile.common.model.documentIA.GenericProperty;
import fr.dossierfacile.common.model.documentIA.ResultModel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/*
This mapper is used to merge multiple analyses Front and Back on a document into a single object.
It uses the @DocumentIAField annotation to map fields from the analyses to the target object.
It prioritizes 2DDoc data over Extraction data and the last analysis over the first one
 */
public class DocumentIAMergerMapper {

    private DocumentIAMergerMapper() {
        // Private constructor to prevent instantiation
    }

    public static <T> Optional<T> map(List<DocumentIAFileAnalysis> documentIAAnalyses, Class<T> targetClass) {
        try {
            T instance = targetClass.getDeclaredConstructor().newInstance();
            var listOf2DDocItems = extract2DDocItems(documentIAAnalyses);
            var listOfExtractionItems = extractExtractionItems(documentIAAnalyses);

            boolean isEmpty = true;
            for (Field field : targetClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(DocumentIAField.class)) {
                    continue;
                }
                DocumentIAField annotation = field.getAnnotation(DocumentIAField.class);
                GenericProperty genericProperty = findProperty(annotation, listOf2DDocItems, listOfExtractionItems);

                if (genericProperty != null) {
                    Object convertedValue = convertValue(genericProperty, annotation.type());
                    convertedValue = applyTransformerIfNeeded(annotation, convertedValue);

                    if (convertedValue != null) {
                        setFieldValue(field, instance, convertedValue);
                        isEmpty = false;
                    }
                }
            }

            return isEmpty ? Optional.empty() : Optional.of(instance);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors du mapping DocumentIA vers " + targetClass.getName(), e);
        }
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

    private static GenericProperty findProperty(DocumentIAField annotation,
                                                List<GenericProperty> listOf2DDocItems,
                                                List<GenericProperty> listOfExtractionItems) {
        GenericProperty genericProperty = null;

        if (!annotation.twoDDocName().isBlank()) {
            genericProperty = listOf2DDocItems.stream()
                    .filter(it -> it.getName().equals(annotation.twoDDocName()))
                    .filter(it -> it.getValue() != null)
                    .findFirst()
                    .orElse(null);
        }

        if (genericProperty == null && !annotation.extractionName().isBlank()) {
            genericProperty = listOfExtractionItems.stream()
                    .filter(it -> it.getName().equals(annotation.extractionName()))
                    .filter(it -> it.getValue() != null)
                    .findFirst()
                    .orElse(null);
        }

        return genericProperty;
    }

    private static Object applyTransformerIfNeeded(DocumentIAField annotation, Object value) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends PropertyTransformer<?,?>> transformerClass = annotation.transformer();
        if (transformerClass == DocumentIAField.NoOpTransformer.class) {
            return value;
        }
        @SuppressWarnings("unchecked") PropertyTransformer<Object, Object> transformer = (PropertyTransformer<Object, Object>) transformerClass.getDeclaredConstructor().newInstance();
        return transformer.transform(value);
    }

    private static void setFieldValue(Field field, Object instance, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static Object convertValue(GenericProperty property, DocumentIAPropertyType type) {
        return switch (type) {
            case STRING -> property.getStringValue();
            case DATE -> property.getDateValue();
            default -> property.getValue();
        };
    }
}