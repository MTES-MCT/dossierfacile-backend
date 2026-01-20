package fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.common.model.documentIA.GenericProperty;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

abstract public class BaseDocumentIAMapper {

    protected <T> Optional<T> instantiate(List<GenericProperty> twoDDocProperties, List<GenericProperty>extractionProperties, Class<T> targetClass) {
        try {
            T instance = targetClass.getDeclaredConstructor().newInstance();

            boolean isEmpty = true;
            for (Field field : targetClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(DocumentIAField.class)) {
                    continue;
                }
                DocumentIAField annotation = field.getAnnotation(DocumentIAField.class);
                GenericProperty genericProperty = findProperty(annotation, twoDDocProperties, extractionProperties);

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

    protected GenericProperty findProperty(DocumentIAField annotation,
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

    private void setFieldValue(Field field, Object instance, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(instance, value);
    }

    private Object convertValue(GenericProperty property, DocumentIAPropertyType type) {
        return switch (type) {
            case STRING -> property.getStringValue();
            case DATE -> property.getDateValue();
            default -> property.getValue();
        };
    }

    private Object applyTransformerIfNeeded(DocumentIAField annotation, Object value) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends PropertyTransformer<?, ?>> transformerClass = annotation.transformer();
        if (transformerClass == DocumentIAField.NoOpTransformer.class) {
            return value;
        }
        @SuppressWarnings("unchecked") PropertyTransformer<Object, Object> transformer = (PropertyTransformer<Object, Object>) transformerClass.getDeclaredConstructor().newInstance();
        return transformer.transform(value);
    }

}
