package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentDeletionSource;
import jakarta.annotation.Nullable;

public interface DocumentDeletionCommonService {

    /**
     * Deletes a document owned by a tenant or by a guarantor, applying the invariants shared by every
     * application: DOCUMENT_DELETED log, owner in-memory collection update, readyForAutoValidation reset,
     * full dossier PDF reset. The status recomputation is left to the caller (see TenantStatusCommonService).
     *
     * @param operatorId BO operator performing the deletion, null otherwise
     * @param source     automatic deletion origin, null for a deletion by the tenant or an operator
     * @return the tenant owning the document (directly or through its guarantor)
     * @throws IllegalStateException for an orphan document (neither tenant nor guarantor): callers must
     *                               filter those out and handle them separately
     */
    Tenant deleteDocument(Document document, @Nullable Long operatorId, @Nullable DocumentDeletionSource source);
}
