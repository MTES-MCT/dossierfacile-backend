package fr.dossierfacile.common.domain.model.tenant;

import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;

import fr.dossierfacile.common.domain.model.DomainAggregate;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Aggregate Root pour le concept de Tenant (Locataire).
 * Cette classe fait partie du modèle de domaine et encapsule l'entité JPA de persistance (TenantEntity)
 * pour en contrôler l'état et protéger les invariants métiers.
 */
@SuppressWarnings("ClassCanBeRecord")
public class Tenant implements Serializable, DomainAggregate<TenantEntity> {

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
    @Override
    public TenantEntity getEntityOnlyForRepository() {
        return this.entity;
    }

    public Long getId() {
        return entity.getId();
    }

    public List<Long> getGuarantorsIds() {
        return entity.getGuarantorIds();
    }

    public Long getApartmentSharingId() {
        return entity.getApartmentSharingId();
    }

    public TenantFileStatus getStatus() {
        return entity.getStatus();
    }

    public void setStatus(TenantFileStatus status) {
        entity.setStatus(status);
    }

    public Boolean getHonorDeclaration() {
        return entity.getHonorDeclaration();
    }

    // --- IDENTITY ACCESSORS ---
    // Replicate the overridden getters of the legacy entity (common/entity/Tenant.java):
    // a THIRD_PARTY (or untyped) tenant displays the tenant_* columns, a SELF tenant the user_account ones.

    public String getFirstName() {
        if (entity.getOwnerType() == TenantOwnerType.SELF) {
            return entity.getFirstName();
        }
        return entity.getTenantFirstName() != null ? entity.getTenantFirstName() : entity.getFirstName();
    }

    public String getLastName() {
        if (entity.getOwnerType() == TenantOwnerType.SELF) {
            return entity.getLastName();
        }
        return entity.getTenantLastName() != null ? entity.getTenantLastName() : entity.getLastName();
    }

    public String getPreferredName() {
        if (entity.getOwnerType() == TenantOwnerType.SELF) {
            return entity.getPreferredName();
        }
        if (entity.getOwnerType() == TenantOwnerType.THIRD_PARTY) {
            return entity.getTenantPreferredName();
        }
        return entity.getPreferredName();
    }

    public String getUserLastName() {
        return entity.getLastName();
    }

    public String getUserPreferredName() {
        return entity.getPreferredName();
    }

    public String getEmail() {
        return entity.getEmail();
    }

    public Boolean getFranceConnect() {
        return entity.getFranceConnect();
    }

    public TenantType getTenantType() {
        return entity.getTenantType();
    }

    public TenantOwnerType getOwnerType() {
        return entity.getOwnerType();
    }

    public String getZipCode() {
        return entity.getZipCode();
    }

    public Boolean getAbroad() {
        return entity.getAbroad();
    }

    public String getClarification() {
        return entity.getClarification();
    }

    public LocalDateTime getLastUpdateDate() {
        return entity.getLastUpdateDate();
    }

    // --- LOGIQUE MÉTIER & COMPORTEMENTS (PROTÈGE LES INVARIANTS) ---

    public void updateLastUpdateDate() {
        entity.setLastUpdateDate(LocalDateTime.now(ZoneId.systemDefault()));
    }
}
