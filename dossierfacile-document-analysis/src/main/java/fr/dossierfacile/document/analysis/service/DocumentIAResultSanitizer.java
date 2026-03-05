package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
                || document == null
                || document.getDocumentCategory() == null) {
            return resultModel;
        }

        var matchingClasses = getFilteredListOfClass(document);
        if (matchingClasses.isEmpty()) {
            return resultModel;
        }

        AllowedSchema allowedSchema = collectAllowedSchema(matchingClasses);
        boolean hasExtractionProperties = resultModel.getExtraction() != null
                && resultModel.getExtraction().getProperties() != null;
        boolean hasWorkToDo = (hasExtractionProperties && !allowedSchema.extractionSchema().isEmpty())
                || (resultModel.getBarcodes() != null && !allowedSchema.twoDDocNames().isEmpty());
        if (!hasWorkToDo) {
            return resultModel;
        }

        var clonedResultModel = resultModel.toBuilder().build();

        if (hasExtractionProperties && !allowedSchema.extractionSchema().isEmpty()) {
            filterExtractionProperties(clonedResultModel, allowedSchema.extractionSchema());
        }
        sanitizeBarcodes(clonedResultModel, allowedSchema.twoDDocNames());
        return clonedResultModel;
    }

    private AllowedSchema collectAllowedSchema(List<Class<?>> matchingClasses) {
        Map<String, ExtractionNode> extractionSchema = new HashMap<>();
        Set<String> allowedTwoDDocNames = new HashSet<>();
        for (Class<?> modelClass : matchingClasses) {
            mergeExtractionSchema(extractionSchema, buildExtractionSchema(modelClass));
            collectTwoDDocNames(allowedTwoDDocNames, modelClass);
        }
        return new AllowedSchema(extractionSchema, allowedTwoDDocNames);
    }

    private void collectTwoDDocNames(Set<String> allowedTwoDDocNames, Class<?> modelClass) {
        for (Field field : modelClass.getDeclaredFields()) {
            DocumentIAField fieldAnnotation = field.getAnnotation(DocumentIAField.class);
            if (fieldAnnotation == null) {
                continue;
            }
            String twoDDocName = fieldAnnotation.twoDDocName();
            if (twoDDocName != null && !twoDDocName.isBlank()) {
                allowedTwoDDocNames.add(twoDDocName);
            }
        }
    }

    private Map<String, ExtractionNode> buildExtractionSchema(Class<?> modelClass) {
        Map<String, ExtractionNode> schema = new HashMap<>();
        for (Field field : modelClass.getDeclaredFields()) {
            DocumentIAField fieldAnnotation = field.getAnnotation(DocumentIAField.class);
            if (fieldAnnotation == null) {
                continue;
            }

            String extractionName = fieldAnnotation.extractionName();
            if (extractionName == null || extractionName.isBlank()) {
                continue;
            }

            ExtractionNode node = buildFieldNode(field, fieldAnnotation.type());
            schema.merge(extractionName, node, this::mergeNodes);
        }
        return schema;
    }

    private ExtractionNode buildFieldNode(Field field, DocumentIAPropertyType type) {
        return switch (type) {
            case OBJECT -> {
                Class<?> nestedType = resolveObjectType(field);
                if (nestedType == null) {
                    yield ExtractionNode.leaf();
                }
                yield new ExtractionNode(buildExtractionSchema(nestedType));
            }
            case LIST_OBJECT -> {
                Class<?> nestedType = resolveListElementType(field);
                if (nestedType == null) {
                    yield ExtractionNode.leaf();
                }
                yield new ExtractionNode(buildExtractionSchema(nestedType));
            }
            default -> ExtractionNode.leaf();
        };
    }

    private Class<?> resolveObjectType(Field field) {
        Class<?> type = field.getType();
        if (type == Object.class) {
            return null;
        }
        return type;
    }

    private Class<?> resolveListElementType(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }

        Type elementType = parameterizedType.getActualTypeArguments()[0];
        if (elementType instanceof Class<?> elementClass) {
            return elementClass;
        }

        if (elementType instanceof ParameterizedType nestedParameterized
                && nestedParameterized.getRawType() instanceof Class<?> rawClass) {
            return rawClass;
        }

        return null;
    }

    private ExtractionNode mergeNodes(ExtractionNode first, ExtractionNode second) {
        if (first.isLeaf()) {
            return second;
        }
        if (second.isLeaf()) {
            return first;
        }

        Map<String, ExtractionNode> merged = new HashMap<>(first.children());
        mergeExtractionSchema(merged, second.children());
        return new ExtractionNode(merged);
    }

    private void mergeExtractionSchema(Map<String, ExtractionNode> target, Map<String, ExtractionNode> source) {
        source.forEach((name, node) -> target.merge(name, node, this::mergeNodes));
    }

    private void filterExtractionProperties(ResultModel resultModel, Map<String, ExtractionNode> extractionSchema) {
        List<GenericProperty> filteredProperties = sanitizePropertyList(resultModel.getExtraction().getProperties(), extractionSchema);
        resultModel.getExtraction().setProperties(filteredProperties);
    }

    private List<GenericProperty> sanitizePropertyList(List<GenericProperty> properties, Map<String, ExtractionNode> schema) {
        if (properties == null || properties.isEmpty()) {
            return List.of();
        }

        List<GenericProperty> sanitized = new ArrayList<>();
        for (GenericProperty property : properties) {
            if (property == null) {
                continue;
            }

            ExtractionNode node = schema.get(property.getName());
            if (node == null) {
                continue;
            }

            sanitizeNestedValue(property, node);
            sanitized.add(property);
        }
        return sanitized;
    }

    private void sanitizeNestedValue(GenericProperty property, ExtractionNode node) {
        if (node.isLeaf()) {
            return;
        }

        Object rawValue = property.getValue();
        if (!(rawValue instanceof List<?> rawList)) {
            return;
        }

        if (GenericProperty.TYPE_OBJECT.equals(property.getType())) {
            List<GenericProperty> nested = toGenericPropertyList(rawList);
            property.setValue(sanitizePropertyList(nested, node.children()));
            return;
        }

        if (!GenericProperty.TYPE_LIST.equals(property.getType())) {
            return;
        }

        List<GenericProperty> listItems = toGenericPropertyList(rawList);
        for (GenericProperty item : listItems) {
            if (!GenericProperty.TYPE_OBJECT.equals(item.getType())) {
                continue;
            }
            Object itemValue = item.getValue();
            if (!(itemValue instanceof List<?> itemList)) {
                continue;
            }
            List<GenericProperty> nested = toGenericPropertyList(itemList);
            item.setValue(sanitizePropertyList(nested, node.children()));
        }
        property.setValue(listItems);
    }

    private List<GenericProperty> toGenericPropertyList(List<?> rawValues) {
        List<GenericProperty> properties = new ArrayList<>();
        for (Object value : rawValues) {
            if (value instanceof GenericProperty genericProperty) {
                properties.add(genericProperty);
            }
        }
        return properties;
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

    private record AllowedSchema(Map<String, ExtractionNode> extractionSchema, Set<String> twoDDocNames) {
    }

    private record ExtractionNode(Map<String, ExtractionNode> children) {
        static ExtractionNode leaf() {
            return new ExtractionNode(Map.of());
        }

        boolean isLeaf() {
            return children == null || children.isEmpty();
        }
    }
}
