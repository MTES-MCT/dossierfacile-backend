# Statut du dossier et règles documentaires

## 1. Documents attendus

| Propriétaire | Catégories obligatoires (`DocumentCategory`) | Documents par catégorie |
|---|---|---|
| Tenant | `IDENTIFICATION`, `RESIDENCY`, `PROFESSIONAL`, `FINANCIAL`, `TAX` | 1, sauf `FINANCIAL` : 1..n (un par source de revenus) |
| Garant `NATURAL_PERSON` | les 5 mêmes | idem |
| Garant `LEGAL_PERSON` | `IDENTIFICATION`, `IDENTIFICATION_LEGAL_PERSON` | 1 |
| Garant `ORGANISM` | `GUARANTEE_PROVIDER_CERTIFICATE` | exactement 1 document au total |

- **Complétude** : `Tenant.isAllCategories()`. Elle teste la **présence** d'au moins un document par catégorie obligatoire ; elle ne compte pas les documents.
- **Cardinalité** : elle découle du finder utilisé par chaque step de `api-tenant/.../register/` : `findFirstByDocumentCategoryAnd…` = un seul document (upsert), `findByDocumentCategoryAnd…AndId` = plusieurs.
- **Garants d'un tenant** : aucun, ou 1 à 2 `NATURAL_PERSON`, ou 1 `LEGAL_PERSON`, ou 1 `ORGANISM`, sans mélange (`MaxGuarantorValidator`).
- **Sous-catégories** : enum `DocumentSubCategory` (groupées par commentaire de catégorie) et `DocumentCategoryStep`. Les valeurs admises par step sont sur les formulaires `register/form/**` (`@DocumentSubcategorySubset`, `@DocumentCategoryStepSubset`).

## 2. Statut d'un document (`DocumentStatus`)

`TO_PROCESS`, `VALIDATED`, `DECLINED` uniquement. Toute modification par le locataire le repasse à `TO_PROCESS` (steps `register/`, `DocumentServiceImpl`). `VALIDATED` / `DECLINED` viennent d'un opérateur BO ou de l'auto-validation ([auto-validation.md](auto-validation.md)).

## 3. Statut d'un tenant (`TenantFileStatus`)

Stocké dans `tenant.status`, mais **toujours recalculé**, en trois temps.

**a. `Tenant.computeStatus()`** : première règle vraie, dans cet ordre, sur les documents du tenant **et de ses garants**.

1. statut actuel `ARCHIVED` → `ARCHIVED`
2. au moins un document `DECLINED` → `DECLINED`
3. pas de déclaration sur l'honneur, ou `isAllCategories()` faux → `INCOMPLETE`
4. au moins un document `TO_PROCESS` → `TO_PROCESS`
5. sinon → `VALIDATED`

**b. `OperatorReviewPolicy.resolveStatus(tenant, computed)`** : ne change que `TO_PROCESS`, qui devient `COMPLETED` si le dossier est dans le périmètre opt-in et qu'aucune revue opérateur n'est accordée. Voir [completed-optin.md](completed-optin.md), [partner-completed-optin.md](partner-completed-optin.md), [tenant-lottery.md](tenant-lottery.md).

## 4. Statut d'un `apartment_sharing`

Non stocké : `ApartmentSharing.getStatus()` le déduit de ses tenants, par priorité `DECLINED` > `INCOMPLETE` > `ARCHIVED` (si tous ; `INCOMPLETE` si certains seulement) > `TO_PROCESS` > `COMPLETED` > `VALIDATED`.
