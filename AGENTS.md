# AGENTS.md — DossierFacile Backend

DossierFacile.fr (service public, Ministère de la Transition écologique) aide les candidats locataires à constituer un dossier de location numérique vérifié.

Le repo héberge aussi **FiligraneFacile**, sous-produit exposant aux usagers uniquement la fusion et le filigrane d'un document. Il est porté par `dossierfacile-api-watermark`, partage du code commun avec DossierFacile (notamment la lib commune `dossierfacile-common-library`) et s'appuie sur `dossierfacile-pdf-generator` pour filigraner les documents.

## Stack technique

- **Langage / framework** : Java 21, Spring Boot, Spring Security (Keycloak OIDC)
- **Base de données** : PostgreSQL + Liquibase (migrations dans `dossierfacile-common-library/src/main/resources/db/`)
- **Messaging** : RabbitMQ (entre producteurs: api-tenant, api-watermark et consommateurs: pdf-generator)
- **Stockage fichiers** : LOCAL (dev) ou stockage compatible S3
- **Tests** : JUnit 5, Mockito, Spring Boot Test, Testcontainers
- **Build** : `mvn clean install` a la racine

## Modules

| Module | Rôle |
|---|---|
| `dossierfacile-api-tenant` | API REST de l'interface locataire (dossier, documents, partage) + API partenaires `DFC` (`.../front/dfc/`) |
| `dossierfacile-api-owner` | API REST de l'interface propriétaires |
| `dossierfacile-api-watermark` | API REST de l'interface FiligraneFacile |
| `dossierfacile-bo` | Back-office (Thymeleaf) — outil des opérateurs |
| `dossierfacile-common-library` | Entités JPA, enums, migrations Liquibase, services partagés |
| `dossierfacile-pdf-generator` | Service de génération PDF (fusion, filigrane) |
| `dossierfacile-task-scheduler` | Tâches planifiées |
| `dossierfacile-document-analysis` | Package contenant les règles d'analyse de documents |

Build : `mvn clean install`. Dev : `mvn spring-boot:run -Dspring-boot.run.profiles=dev,mockOvh` par module.

## Modèle de domaine

Hiérarchie : `apartment_sharing` -> `tenant` -> `guarantor` ; `document` -> `file`.

- **`apartment_sharing` (dossier de candidature)** : concept central regroupant les dossiers locataires. `type` (`ApplicationType`) = `ALONE` / `COUPLE` / `GROUP`. Validé quand **tous** ses tenants le sont.
- **`tenant` (dossier locataire)** : **1 seul par compte utilisateur**. `type` = `CREATE` (principal) / `JOIN` (invité d'un `COUPLE`/`GROUP`). Le principal invite par mail ; il complète le dossier joint **en `COUPLE` uniquement**. Complet = infos (nom/prénom) + 5 documents + déclaration sur l'honneur.
- **`guarantor` (dossier garant)** : 0..n par tenant. `TypeGuarantor` = `NATURAL_PERSON` (≤2) / `ORGANISM` (ex. Visale).
- **`document`** : rattaché à un `tenant` **ou** un `guarantor` (FK exclusives `tenant_id` / `guarantor_id`). `category` (5 : `IDENTIFICATION`, `RESIDENCY`, `PROFESSIONAL`, `FINANCIAL`, `TAX`) + `subCategory`. = **fusion filigranée de plusieurs `file`**. Les catégories `TAX`, `RESIDENCY`, `IDENTIFICATION` et `PROFESSIONAL` sont uniques par `tenant`/`guarantor` en base : remplacer un document existant plutôt que créer un doublon.
- **`file`** : fichier brut (JPG/PNG/PDF). Fusion + filigrane via **traitement asynchrone**.

## Statuts (`status` de `apartment_sharing`, `tenant`, `document`)

- `INCOMPLETE` : infos/documents manquants.
- `TO_PROCESS` : complet, en attente de vérification opérateur.
- `DECLINED` : pièce non conforme à corriger (motifs dans `documentDeniedReasons`) ; resoumission → `TO_PROCESS`.
- `VALIDATED` : vérifié et validé (un `apartment_sharing` l'est quand tous ses tenants le sont).
- `ARCHIVED` : après 3 mois d'inactivité, documents supprimés.

Des **mails automatiques** sont envoyés sur certaines actions (création de compte, demande de modification, validation, archivage…) via un outil de transactionnal emailing (Brevo).

## Points d'attention avant tout dev

Pour limiter les risques de régressions, évaluer l'impact sur chacun de ces axes ; le traiter ou le justifier.

- **Acteurs / canaux** : opérations du back-office (`bo`), API entrante `DFC`, webhooks sortants vers les SI partenaires (`PartnerCallBackService` / `PartnerCallBackType`, sur changement de statut), documentation utilisateur (site + helpdesk Crisp). Ne pas casser les contrats API/webhook.
- **Permissions selon le type de candidature** (`TenantPermissionsService.canAccess`) : accès aux autres dossiers du `apartment_sharing` **uniquement en `COUPLE`** (pas en `GROUP`). Qui accède à un dossier locataire accède aux documents de ses garants. Respecter les permissions existantes.
- **Canaux de partage** (`ApartmentSharingLinkType`) : `LINK`, `MAIL`, `PARTNER`, `OWNER` — couvrir les 4.
- **Bénéficiaire réel** (`TenantOwnerType` = `SELF` / `THIRD_PARTY`) : `user_account ≠ tenant`, ne jamais supposer l'égalité des identités.
- **Changement de statut & partage** : raisonner au niveau `apartment_sharing` (pas seulement `tenant`) — le partage porte sur le `apartment_sharing`, qui regroupe tous les tenants.
- **Rétro-compatibilité `pdf-generator`** : toute modification doit rester rétro-compatible pour `api-tenant` et `api-watermark`, les 2 services backend qui en dépendent.