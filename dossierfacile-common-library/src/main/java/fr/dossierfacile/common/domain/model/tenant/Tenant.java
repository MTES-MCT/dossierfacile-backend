package fr.dossierfacile.common.domain.model.tenant;

import fr.dossierfacile.common.enums.AuthProvider;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.UserType;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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

    // --- ACCESSEURS (LECTURE SEULE POUR PROJECTIONS ET USE CASES) ---

    public Long getId() {
        return entity.getId();
    }

    public String getTenantFirstName() {
        return entity.getTenantFirstName();
    }

    public String getTenantLastName() {
        return entity.getTenantLastName();
    }

    public String getTenantPreferredName() {
        return entity.getTenantPreferredName();
    }

    public Integer getSatisfactionSurvey() {
        return entity.getSatisfactionSurvey();
    }

    public TenantType getTenantType() {
        return entity.getTenantType();
    }

    public String getZipCode() {
        return entity.getZipCode();
    }

    public Boolean getAbroad() {
        return entity.getAbroad();
    }

    public Boolean getHonorDeclaration() {
        return entity.getHonorDeclaration();
    }

    public LocalDateTime getLastUpdateDate() {
        return entity.getLastUpdateDate();
    }

    public String getClarification() {
        return entity.getClarification();
    }

    public TenantFileStatus getStatus() {
        return entity.getStatus();
    }

    public LocalDateTime getOperatorDateTime() {
        return entity.getOperatorDateTime();
    }

    public int getWarnings() {
        return entity.getWarnings();
    }

    public String getOperatorComment() {
        return entity.getOperatorComment();
    }

    public TenantOwnerType getOwnerType() {
        return entity.getOwnerType();
    }

    public String getSearchText() {
        return entity.getSearchText();
    }

    public Long getApartmentSharingId() {
        return entity.getApartmentSharingId();
    }

    // --- DONNÉES DU COMPTE UTILISATEUR (USER_ACCOUNT) ---

    public String getFirstName() {
        return entity.getFirstName();
    }

    public String getLastName() {
        return entity.getLastName();
    }

    public String getPreferredName() {
        return entity.getPreferredName();
    }

    public String getEmail() {
        return entity.getEmail();
    }

    public String getKeycloakId() {
        return entity.getKeycloakId();
    }

    public Boolean getFranceConnect() {
        return entity.getFranceConnect();
    }

    public String getFranceConnectSub() {
        return entity.getFranceConnectSub();
    }

    public String getFranceConnectBirthDate() {
        return entity.getFranceConnectBirthDate();
    }

    public String getFranceConnectBirthPlace() {
        return entity.getFranceConnectBirthPlace();
    }

    public String getFranceConnectBirthCountry() {
        return entity.getFranceConnectBirthCountry();
    }

    public UserType getUserType() {
        return entity.getUserType();
    }

    public LocalDateTime getCreationDate() {
        return entity.getCreationDate();
    }

    public LocalDateTime getLastLoginDate() {
        return entity.getLastLoginDate();
    }

    public boolean isEnabled() {
        return entity.isEnabled();
    }

    public AuthProvider getProvider() {
        return entity.getProvider();
    }

    public String getProviderId() {
        return entity.getProviderId();
    }

    public String getImageUrl() {
        return entity.getImageUrl();
    }

    public String getAcquisitionCampaign() {
        return entity.getAcquisitionCampaign();
    }

    public String getAcquisitionSource() {
        return entity.getAcquisitionSource();
    }

    public String getAcquisitionMedium() {
        return entity.getAcquisitionMedium();
    }

    // --- RÉFÉRENCES PAR ID ---

    public List<Long> getDocumentIds() {
        return entity.getDocumentIds();
    }

    public List<Long> getGuarantorIds() {
        return entity.getGuarantorIds();
    }

    // --- LOGIQUE MÉTIER & COMPORTEMENTS (PROTÈGE LES INVARIANTS) ---

    /**
     * Vérifie si le dossier du locataire est validé.
     */
    public boolean isValidated() {
        return getStatus() == TenantFileStatus.VALIDATED;
    }

    /**
     * Retourne le prénom d'affichage final en gérant le type de bénéficiaire (SELF vs THIRD_PARTY).
     */
    public String getFirstNameForDisplay() {
        if (getOwnerType() == TenantOwnerType.SELF) {
            return getFirstName();
        } else {
            return getTenantFirstName() != null ? getTenantFirstName() : getFirstName();
        }
    }

    /**
     * Retourne le nom d'affichage final en gérant le type de bénéficiaire (SELF vs THIRD_PARTY).
     */
    public String getLastNameForDisplay() {
        if (getOwnerType() == TenantOwnerType.SELF) {
            return getLastName();
        } else {
            return getTenantLastName() != null ? getTenantLastName() : getLastName();
        }
    }

    /**
     * Retourne le nom d'usage d'affichage final.
     */
    public String getPreferredNameForDisplay() {
        if (getOwnerType() == TenantOwnerType.SELF) {
            return getPreferredName();
        }
        if (getOwnerType() == TenantOwnerType.THIRD_PARTY) {
            return getTenantPreferredName();
        }
        return getPreferredName();
    }

    /**
     * Met à jour l'identité du locataire en appliquant des règles d'invariants (impossible si validé).
     */
    public void updateIdentity(String firstName, String lastName, String preferredName) {
        if (isValidated()) {
            throw new IllegalStateException("Impossible de modifier l'identité d'un dossier validé.");
        }
        if (getOwnerType() == null || getOwnerType() == TenantOwnerType.SELF) {
            if (!Boolean.TRUE.equals(getFranceConnect())) {
                entity.setFirstName(firstName);
                entity.setLastName(lastName);
                entity.setPreferredName(preferredName);
            }
        } else {
            entity.setTenantFirstName(firstName);
            entity.setTenantLastName(lastName);
            entity.setTenantPreferredName(preferredName);
        }
        entity.setLastUpdateDate(LocalDateTime.now());
    }

    /**
     * Synchronise le profil avec les données d'identité issues de Keycloak.
     * Retourne true si des changements ont été appliqués.
     */
    public boolean synchronizeWithKeycloak(String keycloakId, String email, String givenName, String familyName,
                                            String preferredUsername, boolean franceConnect, String fcSub,
                                            String fcBirthCountry, String fcBirthPlace, String fcBirthDate) {
        boolean changed = false;

        if (!java.util.Objects.equals(entity.getKeycloakId(), keycloakId)) {
            entity.setKeycloakId(keycloakId);
            changed = true;
        }

        if (!java.util.Objects.equals(entity.getFranceConnect(), franceConnect)) {
            entity.setFranceConnect(franceConnect);
            changed = true;
        }

        if (!java.util.Objects.equals(entity.getFranceConnectSub(), fcSub)) {
            entity.setFranceConnectSub(fcSub);
            changed = true;
        }

        if (!java.util.Objects.equals(entity.getFranceConnectBirthCountry(), fcBirthCountry)) {
            entity.setFranceConnectBirthCountry(fcBirthCountry);
            changed = true;
        }

        if (!java.util.Objects.equals(entity.getFranceConnectBirthPlace(), fcBirthPlace)) {
            entity.setFranceConnectBirthPlace(fcBirthPlace);
            changed = true;
        }

        if (!java.util.Objects.equals(entity.getFranceConnectBirthDate(), fcBirthDate)) {
            entity.setFranceConnectBirthDate(fcBirthDate);
            changed = true;
        }

        // Cas de la liaison FranceConnect ou profil SELF FranceConnecté
        if (franceConnect && getOwnerType() == TenantOwnerType.SELF) {
            if (!java.util.Objects.equals(entity.getFirstName(), givenName)) {
                entity.setFirstName(givenName);
                changed = true;
            }
            if (!java.util.Objects.equals(entity.getLastName(), familyName)) {
                entity.setLastName(familyName);
                changed = true;
            }
            if (preferredUsername != null && !java.util.Objects.equals(entity.getPreferredName(), preferredUsername)) {
                entity.setPreferredName(preferredUsername);
                changed = true;
            }
        }

        if (changed) {
            entity.setLastUpdateDate(LocalDateTime.now());
        }

        return changed;
    }
}
