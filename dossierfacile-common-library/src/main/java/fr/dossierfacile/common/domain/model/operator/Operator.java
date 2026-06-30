package fr.dossierfacile.common.domain.model.operator;

import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import fr.dossierfacile.common.entity.UserRole;

/**
 * Aggregate Root pour le concept d'Opérateur BO.
 * Cette classe fait partie du modèle de domaine et encapsule l'entité JPA de persistance (OperatorEntity)
 * pour en contrôler l'état et protéger les invariants métiers.
 */
@SuppressWarnings("ClassCanBeRecord")
public class Operator implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OperatorEntity entity;

    /**
     * Constructeur public permettant à la couche d'infrastructure (Repository) de construire l'agrégat.
     */
    public Operator(OperatorEntity entity) {
        this.entity = entity;
    }

    /**
     * Permet au Repository de récupérer l'entité interne pour les opérations de persistance (sauvegarde).
     */
    public OperatorEntity getEntity() {
        return this.entity;
    }

    public Long getId() {
        return entity.getId();
    }

    public String getFirstName() {
        return entity.getFirstName();
    }

    public String getLastName() {
        return entity.getLastName();
    }

    public String getEmail() {
        return entity.getEmail();
    }

    public String getKeycloakId() {
        return entity.getKeycloakId();
    }

    public Set<UserRole> getUserRoles() {
        return entity.getUserRoles();
    }
}
