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
            if (className != null) {
                try {
                    classes.add(ClassUtils.forName(className, getClass().getClassLoader()));
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException("Impossible de charger la classe " + className, ex);
                }
            }
        }

        this.documentIaModelClasses = Collections.unmodifiableList(classes);
    }

    // Private package for test
    // Selectionne les modeles DocumentIA applicables au document courant
    // selon la priorite step -> subCategory -> category.
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

    // Construit la vue des champs autorises en fusionnant les schemas de
    // tous les modeles applicables (extraction + 2D doc).
    private AllowedSchema collectAllowedSchema(List<Class<?>> matchingClasses) {
        Map<String, ExtractionNode> extractionSchema = new HashMap<>();
        Set<String> allowedTwoDDocNames = new HashSet<>();
        for (Class<?> modelClass : matchingClasses) {
            mergeExtractionSchema(extractionSchema, buildExtractionSchema(modelClass));
            collectTwoDDocNames(allowedTwoDDocNames, modelClass);
        }
        return new AllowedSchema(extractionSchema, allowedTwoDDocNames);
    }

    // Recupere les noms 2D doc declares dans un modele pour filtrer les barcodes.
    private void collectTwoDDocNames(Set<String> allowedTwoDDocNames, Class<?> modelClass) {
        for (Field field : modelClass.getDeclaredFields()) {
            DocumentIAField fieldAnnotation = field.getAnnotation(DocumentIAField.class);
            if (fieldAnnotation != null) {
                String twoDDocName = fieldAnnotation.twoDDocName();
                if (twoDDocName != null && !twoDDocName.isBlank()) {
                    allowedTwoDDocNames.add(twoDDocName);
                }
            }
        }
    }

    // Produit le schema d'extraction autorise pour un modele, en tenant compte
    // des champs simples et de la structure imbriquee.
    private Map<String, ExtractionNode> buildExtractionSchema(Class<?> modelClass) {
        Map<String, ExtractionNode> schema = new HashMap<>();
        for (Field field : modelClass.getDeclaredFields()) {
            DocumentIAField fieldAnnotation = field.getAnnotation(DocumentIAField.class);
            if (fieldAnnotation != null) {
                String extractionName = fieldAnnotation.extractionName();
                if (extractionName != null && !extractionName.isBlank()) {
                    ExtractionNode node = buildFieldNode(field);
                    schema.merge(extractionName, node, this::mergeNodes);
                }
            }
        }
        return schema;
    }

    // Determine la forme de schema a creer pour un champ: feuille pour un type simple,
    // schema imbrique pour un objet, ou schema d'element pour une liste d'objets.
    private ExtractionNode buildFieldNode(Field field) {
        if (isObjectLikeField(field)) {
            Class<?> nestedType = resolveObjectType(field);
            if (nestedType == null) {
                return ExtractionNode.leaf();
            }
            return new ExtractionNode(buildExtractionSchema(nestedType));
        }

        if (isListObjectLikeField(field)) {
            Class<?> nestedType = resolveListElementType(field);
            if (nestedType == null) {
                return ExtractionNode.leaf();
            }
            return new ExtractionNode(buildExtractionSchema(nestedType));
        }

        return ExtractionNode.leaf();
    }

    // Detecte si un champ represente un objet metier imbrique qui doit etre nettoye recursivement.
    private boolean isObjectLikeField(Field field) {
        Class<?> fieldType = field.getType();
        return !fieldType.isPrimitive()
                && !fieldType.isArray()
                && !List.class.isAssignableFrom(fieldType)
                && fieldType != String.class
                && fieldType != java.time.LocalDate.class
                && !Number.class.isAssignableFrom(fieldType)
                && fieldType != Boolean.class
                && fieldType != Character.class;
    }

    // Detecte si un champ represente une liste d'objets metier a nettoyer element par element.
    private boolean isListObjectLikeField(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) {
            return false;
        }

        Class<?> elementType = resolveListElementType(field);
        if (elementType == null) {
            return false;
        }

        return elementType != String.class
                && elementType != java.time.LocalDate.class
                && !Number.class.isAssignableFrom(elementType)
                && elementType != Boolean.class
                && elementType != Character.class;
    }

    // Resolve la classe concrete d'un objet imbrique pour explorer son schema.
    private Class<?> resolveObjectType(Field field) {
        Class<?> type = field.getType();
        if (type == Object.class) {
            return null;
        }
        return type;
    }

    // Resolve le type d'element d'une liste afin de savoir si la liste porte
    // des objets imbriques et quels sous-champs sont autorises.
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

    // Fusionne deux noeuds de schema afin de conserver l'union des champs autorises.
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

    // Applique la fusion de schema pour tous les champs d'un niveau donne.
    private void mergeExtractionSchema(Map<String, ExtractionNode> target, Map<String, ExtractionNode> source) {
        source.forEach((name, node) -> target.merge(name, node, this::mergeNodes));
    }

    // Lance le filtrage des proprietes d'extraction avec le schema autorise.
    private void filterExtractionProperties(ResultModel resultModel, Map<String, ExtractionNode> extractionSchema) {
        List<GenericProperty> filteredProperties = sanitizePropertyList(resultModel.getExtraction().getProperties(), extractionSchema);
        resultModel.getExtraction().setProperties(filteredProperties);
    }

    // Filtre une liste de proprietes en supprimant celles non autorisees,
    // puis declenche le nettoyage recursif pour les proprietes imbriquees.
    private List<GenericProperty> sanitizePropertyList(List<GenericProperty> properties, Map<String, ExtractionNode> schema) {
        if (properties == null || properties.isEmpty()) {
            return List.of();
        }

        List<GenericProperty> sanitized = new ArrayList<>();
        for (GenericProperty property : properties) {
            if (property != null) {
                ExtractionNode node = schema.get(property.getName());
                if (node != null) {
                    sanitizeNestedValue(property, node);
                    sanitized.add(property);
                }
            }
        }
        return sanitized;
    }

    // Nettoie le contenu interne des proprietes objet et liste d'objets
    // en ne conservant que les sous-champs autorises par le schema.
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
            if (GenericProperty.TYPE_OBJECT.equals(item.getType())) {
                Object itemValue = item.getValue();
                if (itemValue instanceof List<?> itemList) {
                    List<GenericProperty> nested = toGenericPropertyList(itemList);
                    item.setValue(sanitizePropertyList(nested, node.children()));
                }
            }
        }
        property.setValue(listItems);
    }

    // Convertit defensivement une liste brute en liste de GenericProperty
    // pour uniformiser le traitement des structures imbriquees.
    private List<GenericProperty> toGenericPropertyList(List<?> rawValues) {
        List<GenericProperty> properties = new ArrayList<>();
        for (Object value : rawValues) {
            if (value instanceof GenericProperty genericProperty) {
                properties.add(genericProperty);
            }
        }
        return properties;
    }

    // Nettoie les donnees barcode: suppression du rawData et filtrage strict
    // des typedData selon les champs 2D doc autorises.
    private void sanitizeBarcodes(ResultModel resultModel, Set<String> allowedTwoDDocNames) {
        if (allowedTwoDDocNames.isEmpty() || resultModel.getBarcodes() == null) {
            return;
        }
        for (var barcode : resultModel.getBarcodes()) {
            if (barcode != null) {
                barcode.setRawData(null);
                if (barcode.getTypedData() != null) {
                    List<GenericProperty> filteredTypedData = barcode.getTypedData().stream()
                            .filter(property -> property != null && allowedTwoDDocNames.contains(property.getName()))
                            .toList();
                    barcode.setTypedData(filteredTypedData);
                }
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
