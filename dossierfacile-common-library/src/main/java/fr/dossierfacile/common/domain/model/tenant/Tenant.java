package fr.dossierfacile.common.domain.model.tenant;

import fr.dossierfacile.common.infrastructure.entity.TenantEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Aggregate Root pour le concept de Tenant (Locataire).
 * Cette classe fait partie du modèle de domaine et encapsule l'entité JPA de persistance (TenantEntity)
 * pour en contrôler l'état et protéger les invariants métiers.
 */
@SuppressWarnings("ClassCanBeRecord")
public class Tenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TenantEntity entity;

    /**
     * Constructeur public permettant à la couche d'infrastructure (Repository) de construire l'agrégat.
     */
    public Tenant(TenantEntity entity) {
        this.entity = entity;
    }

    /**
     * Permet au Repository de récupérer l'entité interne pour les opérations de persistance (sauvegarde).
     */
    public TenantEntity getEntity() {
        return this.entity;
    }

    public Long getId() {
        return entity.getId();
    }

    public Long getApartmentSharingId() {
        return entity.getApartmentSharingId();
    }

    // --- LOGIQUE MÉTIER & COMPORTEMENTS (PROTÈGE LES INVARIANTS) ---

    public void updateLastUpdateDate() {
        entity.setLastUpdateDate(LocalDateTime.now(ZoneId.systemDefault()));
    }
}
