package fr.dossierfacile.common.domain.model.guarantor;

import fr.dossierfacile.common.domain.model.DomainAggregate;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.infrastructure.entity.GuarantorEntity;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Aggregate / Domain Model pour le concept de Guarantor (Garant).
 * Encapsule l'entité JPA de persistance (GuarantorEntity).
 */
@SuppressWarnings("ClassCanBeRecord")
public class Guarantor implements Serializable, DomainAggregate<GuarantorEntity> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final GuarantorEntity entity;

    public Guarantor(GuarantorEntity entity) {
        this.entity = entity;
    }

    @Override
    public GuarantorEntity getEntityOnlyForRepository() {
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

    public String getPreferredName() {
        return entity.getPreferredName();
    }

    public TypeGuarantor getTypeGuarantor() {
        return entity.getTypeGuarantor();
    }

    public Long getTenantId() {
        return entity.getTenantId();
    }

    public String getLegalPersonName() {
        return entity.getLegalPersonName();
    }

    public List<Long> getDocumentIds() {
        return entity.getDocumentIds();
    }

    public String getCompleteName() {
        StringBuilder fullName = new StringBuilder();
        if (entity.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON) {
            if (StringUtils.isNotBlank(entity.getFirstName())) {
                fullName.append(entity.getFirstName());
            }
            if (StringUtils.isNotBlank(entity.getLastName())) {
                fullName.append(" ").append(entity.getLastName());
            }
        } else if (entity.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON && StringUtils.isNotBlank(entity.getLegalPersonName())) {
            fullName.append(entity.getLegalPersonName());
        }
        return fullName.toString();
    }
}
