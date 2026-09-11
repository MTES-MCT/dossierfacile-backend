package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import org.mapstruct.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for the MapStruct mappers serving both the tenant itself and partners:
 * extending it makes every mapped {@link TenantFileStatus} field go through the
 * defensive COMPLETED masking when a partner context is present and the partner did not
 * opt in to the COMPLETED status.
 * <p>
 * TODO(partner-completed-optin-100): once every partner has integrated the COMPLETED status, delete this
 * class, the {@code toPartnerVisibleStatus} usages in ApplicationFullMapper and
 * TenantMapper, and the {@code setOperatorReviewPolicy} calls in their tests.
 */
public abstract class MasksCompletedStatusForPartner {

    protected OperatorReviewPolicy operatorReviewPolicy;

    @Autowired
    public void setOperatorReviewPolicy(OperatorReviewPolicy operatorReviewPolicy) {
        this.operatorReviewPolicy = operatorReviewPolicy;
    }

    public TenantFileStatus toPartnerVisibleStatus(TenantFileStatus status, @Context UserApi userApi) {
        // Status checked first: the partner opt-in (a flag lookup) is only read for COMPLETED
        if (userApi == null || status != TenantFileStatus.COMPLETED || operatorReviewPolicy.isPartnerOptedIn(userApi)) {
            return status;
        }
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName());
    }
}
