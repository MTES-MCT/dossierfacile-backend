# Module d'Auto-Validation des Dossiers (Bot d'Auto-Validation)

## 1. Vue d'ensemble

Le module d'**Auto-Validation** de DossierFacile permet d'approuver automatiquement et sans intervention humaine les dossiers de candidature dont l'ensemble des documents restants en attente de traitement (`TO_PROCESS`) sont éligibles à la validation automatique (ex: attestation de garantie Visale) et répondent à 100% aux critères d'analyse automatique (`DocumentAnalysisReport`).

Ce système garantit :
- **Un gain de temps immédiat pour l'usager** (dossiers validés en autonomie).
- **L'isolation stricte des files d'attente BO** : aucun dossier éligible à l'auto-validation n'est distribué aux opérateurs humains tant que le bot n'a pas statué.
- **Un fallback immédiat et transparent vers l'humain** : si un document ne peut être validé automatiquement (rapport incomplet, règles en échec, pièce non éligible...), le dossier retombe instantanément dans la pile des opérateurs humains.
- **Une traçabilité analytics complète (Metabase / DBT)** via des DTOs typés et des logs dédiés dans `tenant_log`.

---

## 2. Modèle de Données & Migrations Liquibase

### 2.1 Champ `ready_for_auto_validation` sur la table `tenant`
Un champ boolean a été ajouté sur la table `tenant` :
- Nom : `ready_for_auto_validation`
- Type : `boolean`
- Valeur par défaut : `false`
- Migration : `20260723160000-add-ready-for-auto-validation-to-tenant.xml`

### 2.2 Exclusion de la Vue Matérialisée `ranked_tenant`
Pour garantir qu'un opérateur humain ne reçoive jamais un dossier en cours de file d'auto-validation, la vue matérialisée `ranked_tenant` exclut les dossiers ayant `ready_for_auto_validation = true`.

- Migration : `20260731140000-update-ranked-tenant-exclude-auto-validation.xml`
- **Procédure de Down / Rollback Liquibase** intégrée dans la migration pour restaurer la définition précédente de la vue en cas de besoin.

---

## 3. Règles d'Éligibilité & Calcul Dynamique

### 3.1 Règle de Calcul Dynamique (`isTenantReadyForAutoValidation`)
Un dossier candidat (`Tenant`) est positionné à `ready_for_auto_validation = true` si et seulement si toutes les conditions suivantes sont réunies :
1. La déclaration sur l'honneur est cochée (`honorDeclaration = true`) et toutes les catégories obligatoires sont complètes (`isAllCategories()`).
2. **Aucun** document du dossier n'est en statut `DECLINED`.
3. Il existe **au moins 1 document** en statut `TO_PROCESS`.
4. **TOUS** les documents actuellement au statut `TO_PROCESS` (du locataire et de ses garants) sont éligibles à la validation automatique (`isEligibleForAutoValidation`).

### 3.2 Documents Éligibles
Actuellement, les documents éligibles à l'auto-validation sont :
- Categorie / Sous-catégorie : `DocumentSubCategory.VISALE` (Garantie Visale).

### 3.3 Réinitialisation à `false`
Le drapeau `ready_for_auto_validation` est automatiquement réinitialisé à `false` dans les cas suivants :
- Ajout, modification ou suppression d'un document ou fichier.
- Fin du traitement du bot d'auto-validation (que le dossier soit validé ou renvoyé aux opérateurs).

---

## 4. Composants Logiciels & Architecture

```
                                  +---------------------------------------+
                                  |        dossierfacile-api-tenant       |
                                  |  AbstractDocumentSaveStep / Services  |
                                  +-------------------+-------------------+
                                                      |
                                     Positionne `ready_for_auto_validation`
                                                      |
                                                      v
+---------------------------------------------------------------------------------------------------+
|                                  dossierfacile-common-library                                     |
|                                                                                                   |
|  TenantAutoValidationService / TenantAutoValidationServiceImpl                                    |
|   ├─ isEligibleForAutoValidation(Document)                                                        |
|   ├─ isTenantReadyForAutoValidation(Tenant)                                                       |
|   ├─ listTenantsToAutoValidate(LocalDateTime)                                                     |
|   └─ processAutoValidationForTenant(Long tenantId)  [@Transactional (1 transaction per Tenant)]   |
+---------------------------------------------------------------------------------------------------+
                                                      ^
                                                      |
                                     Interroge et traite la file d'attente
                                                      |
                                  +-------------------+-------------------+
                                  |     dossierfacile-task-scheduler      |
                                  |      TenantAutoValidationTask         |
                                  +---------------------------------------+
```

### 4.1 Modules & Responsabilités

| Module | Composant | Rôle |
|---|---|---|
| `dossierfacile-common-library` | `TenantAutoValidationService` | Interface du service d'auto-validation. |
| `dossierfacile-common-library` | `TenantAutoValidationServiceImpl` | Implémentation du calcul d'éligibilité, de l'évaluation des rapports d'analyse et du processus d'auto-validation transactionnel. |
| `dossierfacile-common-library` | `TenantCommonRepository` | Requêtes d'exclusion BO et méthode `findTenantsToAutoValidate`. |
| `dossierfacile-api-tenant` | `AbstractDocumentSaveStep` | Recalcule et met à jour `ready_for_auto_validation` lors du dépôt de document. |
| `dossierfacile-api-tenant` | `DocumentServiceImpl` / `FileServiceImpl` | Réinitialise `ready_for_auto_validation = false` lors de la suppression de pièces/fichiers. |
| `dossierfacile-bo` | `TenantService` | Exclut les dossiers flaggués des compteurs et de la distribution aux opérateurs humains. |
| `dossierfacile-task-scheduler` | `TenantAutoValidationTask` | Tâche planifiée périodique qui récupère et traite les dossiers prêts. |

---

## 5. Pipeline d'Exécution de la Tâche Planifiée

La tâche planifiée `TenantAutoValidationTask` s'exécute selon le workflow suivant :

1. **Extraction de la File** :
   - Récupère les dossiers avec `ready_for_auto_validation = true`, `status = TO_PROCESS`, non modifiés depuis plus de `X` minutes (`tenant.auto.validation.tenant-min-age-minutes`, par défaut 30 min).
2. **Traitement Transactionnel par Locataire** :
   - Pour chaque dossier, une transaction isolée (`@Transactional`) est ouverte via `processAutoValidationForTenant(tenantId)`.
3. **Évaluation des Documents `TO_PROCESS`** :
   - Le bot parcourt **tous** les documents en `TO_PROCESS` et évalue leur `DocumentAnalysisReport` :
     - Le rapport doit exister (`report != null`).
     - Le statut d'analyse doit être `DocumentAnalysisStatus.CHECKED`.
     - Les règles échouées (`failedRules`) et inconcluantes (`inconclusiveRules`) doivent être vides.
     - Les règles passées (`passedRules`) doivent être non vides.
4. **Décision et Actions** :
   - **En cas de succès (`VALIDATED`)** :
     - Les documents passent à `DocumentStatus.VALIDATED`.
     - `ready_for_auto_validation` repasse à `false`.
     - Appelle `tenantCommonService.changeTenantStatusToValidated(tenant)` (validation du dossier, envoi des webhooks partenaires et des e-mails usagers).
     - Enregistre `TenantLog(ACCOUNT_VALIDATED)` et `TenantLog(ACCOUNT_AUTOMATICALLY_VALIDATED)`.
   - **En cas d'échec ou fallback (`FAILED` / `NO_DOCUMENTS`)** :
     - `ready_for_auto_validation` repasse à `false`.
     - Le statut reste `TO_PROCESS`.
     - Le dossier redevient instantanément éligible pour la file des opérateurs humains.
     - Enregistre `TenantLog(ACCOUNT_AUTO_VALIDATION_FAILED)`.

---

## 6. Structuration des Logs & Analytics (Metabase / DBT)

Pour permettre un suivi analytique précis sur Metabase via DBT, les détails sont consignés dans la colonne JSONB `log_details` de la table `tenant_log` en s'appuyant sur des objets DTO typés Java.

### 6.1 Enums Dédiés

#### `AutoValidationResultStatus` (`fr.dossierfacile.common.enums`)
- `VALIDATED` : Validation automatique réussie.
- `FAILED` : Échec des critères d'auto-validation (fallback humain).
- `NO_DOCUMENTS` : Aucun document en attente.

#### `DocumentAutoValidationReason` (`fr.dossierfacile.common.enums`)
- `VALIDATED` : Document validé par le bot.
- `DOCUMENT_NOT_ELIGIBLE` : Document non éligible à l'auto-validation.
- `REPORT_MISSING` : Absence de rapport d'analyse.
- `REPORT_NOT_CHECKED` : Statut du rapport différent de `CHECKED`.
- `FAILED_RULES_PRESENT` : Présence de règles en échec.
- `INCONCLUSIVE_RULES_PRESENT` : Présence de règles inconcluantes.
- `NO_PASSED_RULES` : Aucune règle passée avec succès.

### 6.2 Modèles DTO (`fr.dossierfacile.common.model.log`)

- `AutoValidationLogDetails` :
  - `status` (`AutoValidationResultStatus`)
  - `documents` (`List<AutoValidationDocumentDetail>`)
- `AutoValidationDocumentDetail` :
  - `documentId` (`Long`)
  - `documentCategory` (`DocumentCategory`)
  - `documentSubCategory` (`DocumentSubCategory`)
  - `documentCategoryStep` (`DocumentCategoryStep`)
  - `reason` (`DocumentAutoValidationReason`)

### 6.3 Exemple de Payload JSONB (`tenant_log.log_details`)

**Succès (`ACCOUNT_AUTOMATICALLY_VALIDATED`)** :
```json
{
  "status": "VALIDATED",
  "documents": [
    {
      "documentId": 12345,
      "documentCategory": "FINANCIAL",
      "documentSubCategory": "VISALE",
      "documentCategoryStep": "UNDEFINED",
      "reason": "VALIDATED"
    }
  ]
}
```

**Fallback (`ACCOUNT_AUTO_VALIDATION_FAILED`)** :
```json
{
  "status": "FAILED",
  "documents": [
    {
      "documentId": 12345,
      "documentCategory": "FINANCIAL",
      "documentSubCategory": "VISALE",
      "documentCategoryStep": "UNDEFINED",
      "reason": "FAILED_RULES_PRESENT"
    }
  ]
}
```

---

## 7. Propriétés de Configuration (`application.properties`)

Dans `dossierfacile-task-scheduler/src/main/resources/application.properties` :

```properties
# Période d'exécution de la tâche planifiée (en ms) - Défaut: 5 min (300 000 ms)
tenant.auto.validation.task.fixed-delay-ms=300000

# Ancienneté minimale du dossier flaggué avant d'être traité (en minutes) - Défaut: 30 min
tenant.auto.validation.tenant-min-age-minutes=30
```
