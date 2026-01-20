package fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper;

@FunctionalInterface
public interface PropertyTransformer<I, O> {
    O transform(I input);
}
