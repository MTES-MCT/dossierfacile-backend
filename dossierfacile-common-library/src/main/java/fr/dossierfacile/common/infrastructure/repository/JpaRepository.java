package fr.dossierfacile.common.infrastructure.repository;

/**
 * Interface de marquage commune pour identifier tous les JpaRepository personnalisés
 * (les wrappers du domaine qui encapsulent les repositories Spring Data JPA de l'infrastructure).
 * Utilisée notamment par les tests d'architecture (ArchUnit) pour valider les règles d'encapsulation.
 */
public interface JpaRepository {
}
