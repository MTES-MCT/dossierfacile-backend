package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentRule {

    R_TAX_PARSE(Level.WARN,"La lecture des informations de l'avis a échoué"),
    R_TAX_FAKE(Level.CRITICAL,"Les informations sont floues ou corrompues"),
    R_TAX_N1( Level.CRITICAL, "L'avis d'imposition sur les revenus N-1 doit etre fournis"),
    R_TAX_LEAF( Level.CRITICAL, "Veuillez fournir les feuillets des avis"),
    R_TAX_ALL_LEAF( Level.WARN, "Veuillez fournir tous les feuillets des avis"),// feuillet 1 founi
    R_TAX_N3( Level.CRITICAL, "Les avis d'imposition antérieur à N-3 ne sont pas autorisé"),
    R_TAX_NAMES( Level.WARN, "Les noms et prénoms ne correspondent pas");

    public enum Level{
        CRITICAL, WARN
    }

    private final Level level;
    private final String defaultMessage;

}

