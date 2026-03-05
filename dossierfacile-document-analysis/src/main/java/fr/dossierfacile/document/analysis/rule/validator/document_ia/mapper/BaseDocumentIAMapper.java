package fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public abstract class BaseDocumentIAMapper {

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
                    Object convertedValue = convertValue(field, genericProperty, annotation.type());
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

    private Object convertValue(Field targetField, GenericProperty property, DocumentIAPropertyType type) {
        return switch (type) {
            case STRING -> property.getStringValue();
            case DATE -> property.getDateValue();
            case LIST_STRING -> property.getStringListValue();
            case OBJECT -> mapNestedObject(targetField, property);
            case LIST_OBJECT -> mapNestedObjectList(targetField, property);
        };
    }

    private Object mapNestedObject(Field targetField, GenericProperty property) {
        List<GenericProperty> nestedProperties = property.getObjectValue();
        if (nestedProperties == null || nestedProperties.isEmpty()) {
            return null;
        }

        // Reuse the same mapper pipeline to support nested models with @DocumentIAField.
        return instantiate(List.of(), nestedProperties, targetField.getType()).orElse(null);
    }

    private Object mapNestedObjectList(Field targetField, GenericProperty property) {
        List<List<GenericProperty>> listOfNestedProperties = property.getObjectListValue();
        if (listOfNestedProperties == null || listOfNestedProperties.isEmpty()) {
            return List.of();
        }

        Class<?> nestedClass = resolveListElementType(targetField);

        return listOfNestedProperties.stream()
                .map(nestedProperties -> instantiate(List.of(), nestedProperties, nestedClass).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private Class<?> resolveListElementType(Field targetField) {
        Type genericType = targetField.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("Field '" + targetField.getName() + "' must be parameterized to use LIST_OBJECT");
        }

        Type elementType = parameterizedType.getActualTypeArguments()[0];
        if (elementType instanceof Class<?> elementClass) {
            return elementClass;
        }

        if (elementType instanceof ParameterizedType nestedParameterized && nestedParameterized.getRawType() instanceof Class<?> rawClass) {
            return rawClass;
        }

        throw new IllegalStateException("Unable to resolve list element type for field '" + targetField.getName() + "'");
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
