package fr.dossierfacile.process.file.service.document_rules.validator.document_ia.mapper;

import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.DocumentIAPropertyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DocumentIAField {
    /**
     * Nom du champ dans le 2D-Doc. Optionnel, vide par défaut.
     */
    String twoDDocName() default "";

    /**
     * Nom du champ dans l'extraction. Optionnel, vide par défaut.
     */
    String extractionName() default "";

    DocumentIAPropertyType type() default DocumentIAPropertyType.STRING;

    /**
     * Classe de transformation optionnelle pour convertir la valeur extraite.
     * La classe doit implémenter Function<Object, Object> ou être un PropertyTransformer.
     */
    Class<? extends PropertyTransformer<?, ?>> transformer() default NoOpTransformer.class;

    /**
     * Transformer par défaut qui ne fait rien (identité)
     */
    class NoOpTransformer implements PropertyTransformer<Object, Object> {

        @Override
        public Object transform(Object input) {
            return input;
        }
    }
}
