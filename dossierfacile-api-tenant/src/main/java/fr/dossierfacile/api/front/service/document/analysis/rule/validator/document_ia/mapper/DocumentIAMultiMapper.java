package fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.model.documentIA.BarcodeModel;
import fr.dossierfacile.common.model.documentIA.GenericProperty;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/*
This mapper is used to return a list of multiple analyses on a document.
It uses the @DocumentIAField annotation to map fields from the analyses to the target object.
It prioritizes 2DDoc data over Extraction data
 */
public class DocumentIAMultiMapper {

    private record AnalysisProperties(List<GenericProperty> doc2DProperties,
                                      List<GenericProperty> extractionProperties) {
    }

    private DocumentIAMultiMapper() {
        // Private constructor to prevent instantiation
    }

    public static <T> List<T> map(List<DocumentIAFileAnalysis> documentIAAnalyses, Class<T> targetClass) {
        try {
            var listOfResults = new ArrayList<T>();

            for (DocumentIAFileAnalysis documentIAFileAnalysis : documentIAAnalyses) {
                T instance = targetClass.getDeclaredConstructor().newInstance();
                List<GenericProperty> doc2DProperties = extract2DDocItems(documentIAFileAnalysis);
                List<GenericProperty> extractionProperties = extractExtractionItems(documentIAFileAnalysis);

                boolean isEmpty = true;

                for (Field field : targetClass.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(DocumentIAField.class)) {
                        continue;
                    }
                    DocumentIAField annotation = field.getAnnotation(DocumentIAField.class);
                    GenericProperty genericProperty = findProperty(annotation, doc2DProperties, extractionProperties);

                    if (genericProperty != null) {
                        Object convertedValue = convertValue(genericProperty, annotation.type());
                        convertedValue = applyTransformerIfNeeded(annotation, convertedValue);

                        if (convertedValue != null) {
                            setFieldValue(field, instance, convertedValue);
                            isEmpty = false;
                        }
                    }
                }
                if (!isEmpty) {
                    listOfResults.add(instance);
                }
            }

            return listOfResults;
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors du mapping DocumentIA vers " + targetClass.getName(), e);
        }
    }

    private static List<GenericProperty> extract2DDocItems(DocumentIAFileAnalysis documentIAAnalyse) {
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

    private static List<GenericProperty> extractExtractionItems(DocumentIAFileAnalysis documentIAAnalyse) {
        return documentIAAnalyse
                .getResult()
                .getExtraction()
                .getProperties();
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
        Class<? extends PropertyTransformer<?, ?>> transformerClass = annotation.transformer();
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