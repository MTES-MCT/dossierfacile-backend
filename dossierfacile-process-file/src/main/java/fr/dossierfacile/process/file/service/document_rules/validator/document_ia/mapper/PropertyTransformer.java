package fr.dossierfacile.process.file.service.document_rules.validator.document_ia.mapper;

@FunctionalInterface
public interface PropertyTransformer<I, O> {
    O transform(I input);
}
