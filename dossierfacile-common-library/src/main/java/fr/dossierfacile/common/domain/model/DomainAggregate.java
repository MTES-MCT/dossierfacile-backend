package fr.dossierfacile.common.domain.model;

/**
 * Interface commune pour tous les aggregators (Aggregate Roots) du domaine.
 * Elle permet d'accéder à l'entité JPA sous-jacente uniquement pour les besoins de persistance.
 *
 * @param <E> Le type de l'entité JPA encapsulée.
 */
public interface DomainAggregate<E> {
    E getEntityOnlyForRepository();
}
