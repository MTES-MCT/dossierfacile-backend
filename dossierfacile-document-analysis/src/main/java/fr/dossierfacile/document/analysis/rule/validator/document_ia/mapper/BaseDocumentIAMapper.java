package fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.dossierfacile.common.model.document_ia.GenericProperty;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseDocumentIAMapper {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    protected <T> Optional<T> instantiate(List<GenericProperty> twoDDocProperties, List<GenericProperty> extractionProperties, Class<T> targetClass) {
        try {
            var valuesByField = objectMapper.createObjectNode();
            boolean isEmpty = true;

            for (Field field : targetClass.getDeclaredFields()) {
                Optional<Object> mappedValue = mapAnnotatedFieldValue(field, twoDDocProperties, extractionProperties);
                if (mappedValue.isPresent()) {
                    valuesByField.set(field.getName(), objectMapper.valueToTree(mappedValue.get()));
                    isEmpty = false;
                }
            }

            if (isEmpty) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.treeToValue(valuesByField, targetClass));
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors du mapping DocumentIA vers " + targetClass.getName(), e);
        }
    }

    // Mappe un champ annote @DocumentIAField en appliquant selection de source,
    // transformation eventuelle puis conversion vers le type du champ.
    private Optional<Object> mapAnnotatedFieldValue(Field field,
                                                    List<GenericProperty> twoDDocProperties,
                                                    List<GenericProperty> extractionProperties)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!field.isAnnotationPresent(DocumentIAField.class)) {
            return Optional.empty();
        }

        DocumentIAField annotation = field.getAnnotation(DocumentIAField.class);
        GenericProperty genericProperty = findProperty(annotation, twoDDocProperties, extractionProperties);
        if (genericProperty == null) {
            return Optional.empty();
        }

        Object rawValue = hasCustomTransformer(annotation)
                ? extractRawValueForTransformer(genericProperty)
                : extractRawValueForField(field, genericProperty);
        Object transformedValue = applyTransformerIfNeeded(annotation, rawValue);
        Object convertedValue = convertToFieldType(field, transformedValue);

        return Optional.ofNullable(convertedValue);
    }

    private boolean hasCustomTransformer(DocumentIAField annotation) {
        return annotation.transformer() != DocumentIAField.NoOpTransformer.class;
    }

    private Object extractRawValueForTransformer(GenericProperty property) {
        Object value = property.getValue();
        return value == null ? null : value.toString();
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

    private Object extractRawValueForField(Field targetField, GenericProperty property) {
        Class<?> fieldType = targetField.getType();

        if (fieldType == String.class) {
            Object value = property.getValue();
            return value == null ? null : value.toString();
        }

        if (fieldType.isArray() && fieldType.getComponentType() == String.class) {
            return normalizeStringCollectionRawValue(property.getValue());
        }

        if (List.class.isAssignableFrom(fieldType)) {
            return extractListValue(targetField, property);
        }

        if (!isSimpleType(fieldType)) {
            return mapNestedObject(targetField, property);
        }

        return property.getValue();
    }

    private Object convertToFieldType(Field field, Object value) {
        if (value == null) {
            return null;
        }
        JavaType targetType = objectMapper.getTypeFactory().constructType(field.getGenericType());
        return objectMapper.convertValue(value, targetType);
    }


    private Object extractListValue(Field targetField, GenericProperty property) {
        Class<?> elementType = resolveListElementType(targetField);

        if (elementType == String.class) {
            return normalizeStringCollectionRawValue(property.getValue());
        }

        return mapNestedObjectList(targetField, property);
    }

    private Object mapNestedObject(Field targetField, GenericProperty property) {
        List<GenericProperty> nestedProperties = property.getObjectValue();
        if (nestedProperties == null || nestedProperties.isEmpty()) {
            return null;
        }

        return instantiate(List.of(), nestedProperties, targetField.getType()).orElse(null);
    }

    private List<Object> mapNestedObjectList(Field targetField, GenericProperty property) {
        List<List<GenericProperty>> listOfNestedProperties = property.getObjectListValue();
        if (listOfNestedProperties == null || listOfNestedProperties.isEmpty()) {
            return List.of();
        }

        Class<?> nestedClass = resolveListElementType(targetField);
        List<Object> mapped = new ArrayList<>();
        for (List<GenericProperty> nestedProperties : listOfNestedProperties) {
            instantiate(List.of(), nestedProperties, nestedClass).ifPresent(mapped::add);
        }
        return mapped;
    }

    private Class<?> resolveListElementType(Field targetField) {
        Type genericType = targetField.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("Field '" + targetField.getName() + "' must be parameterized to use list mapping");
        }

        Type elementType = parameterizedType.getActualTypeArguments()[0];
        if (elementType instanceof Class<?> elementClass) {
            return elementClass;
        }

        if (elementType instanceof ParameterizedType nestedParameterized
                && nestedParameterized.getRawType() instanceof Class<?> rawClass) {
            return rawClass;
        }

        throw new IllegalStateException("Unable to resolve list element type for field '" + targetField.getName() + "'");
    }

    private boolean isSimpleType(Class<?> fieldType) {
        return fieldType.isPrimitive()
                || Number.class.isAssignableFrom(fieldType)
                || fieldType == Boolean.class
                || fieldType == Character.class
                || fieldType == java.time.LocalDate.class;
    }

    private Object applyTransformerIfNeeded(DocumentIAField annotation, Object value)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends PropertyTransformer<?, ?>> transformerClass = annotation.transformer();
        if (transformerClass == DocumentIAField.NoOpTransformer.class) {
            return value;
        }

        @SuppressWarnings("unchecked")
        PropertyTransformer<Object, Object> transformer =
                (PropertyTransformer<Object, Object>) transformerClass.getDeclaredConstructor().newInstance();
        return transformer.transform(value);
    }

    private List<String> normalizeStringCollectionRawValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream().map(it -> it == null ? null : it.toString()).toList();
        }
        return List.of(value.toString());
    }
}
