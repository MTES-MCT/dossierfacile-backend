# POC « Statut étudiant à la source » (DAS MESR)

Récupération du statut étudiant d'un locataire FranceConnecté auprès du ministère de l'Enseignement supérieur
(MESR) via API Particulier, et génération d'un justificatif PDF ajouté au document PROFESSIONAL / STUDENT.

Périmètre du POC : preprod, dossier seul (`ALONE`), locataire FranceConnecté, document relu par un opérateur.
Hors périmètre : garant, couple/colocation, auto-validation, mise en forme métier du PDF.

## Flux

```
tenantv3 ──keycloak.login(idpHint=oidc, ?mesri=1)──▶ Keycloak ──/authorize scopes mesri_*──▶ FranceConnect
tenantv3 ◀── JWT ── Keycloak ◀── access_token FC (60 s, storeToken) ◀── FranceConnect
tenantv3 ──POST /api/register/documentProfessional/mesri (Bearer JWT)──▶ api-tenant
api-tenant ──GET /realms/dossier-facile/broker/oidc/token (Bearer JWT)──▶ Keycloak ──▶ access_token FC
api-tenant ──GET {base}/v3/mesri/statut_etudiant/france_connect?recipient=SIRET (Bearer FC)──▶ API Particulier ──▶ MESR
api-tenant : PDF « Relevé de statut étudiant » → saveStepRegister(DOCUMENT_PROFESSIONAL, STUDENT) → TenantLog
```

Le jeton FranceConnect vit 60 secondes et n'est pas rafraîchi : le front appelle le backend dès le retour de
FranceConnect. Un seul secret est en jeu, le contrat FranceConnect de Keycloak ; API Particulier n'a besoin
d'aucune clé en mode FranceConnect.

## Code (api-tenant, package `fr.dossierfacile.api.front.das.mesri`)

| Classe | Rôle |
|---|---|
| `FranceConnectTokenServiceImpl` | lit le jeton FC stocké par Keycloak (`/broker/oidc/token`), vérifie `accessTokenExpiration` |
| `ApiParticulierClientImpl` | appel HTTP API Particulier, mapping des statuts en exceptions |
| `MesriStudentStatus` | réponse brute + admissions parsées |
| `MesriStudentStatusPdfRenderer` | « Relevé de statut étudiant » (maquette DF) : logos RF/DossierFacile, titulaire, période déclarée, une carte par admission (plus récente d'abord), pied de page avec source et numéro de page ; Marianne, pagination automatique |
| `StudentStatementHolder` | titulaire du relevé : bloc `identite` du MESR si servi, sinon identité FranceConnect du locataire |
| `ByteArrayMultipartFile` | fichier en mémoire pour réutiliser le chemin d'upload |
| `MesriStudentEligibility` | règles d'éligibilité (API configurée, FranceConnecté, dossier seul, flag actif pour l'usager) ; expose `TenantModel.mesriStudentEligible` via `TenantMapper` |
| `MesriStudentDocumentService` | orchestration, sauvegarde via `saveStepRegister` |
| `MesriStudentLogService` | `TenantLog` succès / échec (échec en transaction séparée) |
| `RegisterController.documentProfessionalFromMesri` | `POST /api/register/documentProfessional/mesri` |

Codes HTTP renvoyés au front :

| Code | Cas | Exception |
|---|---|---|
| 200 | document ajouté, `TenantModel` à jour | |
| 403 | flag désactivé, non FranceConnecté, dossier non `ALONE` | `MesriNotEligibleException` |
| 404 | étudiant inconnu du MESR (code API `25003`) | `MesriStudentNotFoundException` |
| 409 | jeton FC absent, expiré ou refusé (code API `51502`, Keycloak 403) : relancer FranceConnect | `FranceConnectTokenUnavailableException` |
| 422 | 200 sans aucune admission | `MesriNoAdmissionException` |
| 502 | autre réponse ou erreur réseau API Particulier | `ApiParticulierException` |

401 est volontairement évité : l'intercepteur axios du front le traite comme une session perdue.

## Configuration

Propriétés api-tenant :

```
api-particulier.base-url=https://staging.particulier.api.gouv.fr   # vide = fonctionnalité inactive (502)
api-particulier.recipient-siret=12006701200027                     # SIRET DGALN, donnée publique
keycloak.server.url / keycloak.server.realm                        # déjà présents
```

Feature flag DB : `das-mesri-student` (migration `20261016000000-add-das-mesri-student-flag.xml`, inactif par
défaut, à activer dans le BO).

Keycloak (console admin, non exporté) :

- IdP `oidc` → `defaultScope` += `mesri_identite mesri_admissions mesri_admission_inscrit
  mesri_admission_regime_formation mesri_admission_commune_etudes mesri_admission_etablissement_etudes`.
- Rôle client `broker` / `read-token` dans `default-roles-dossier-facile` (l'utilisateur doit l'avoir).
- Le client Keycloak du front doit porter ce rôle dans son scope (`fullScopeAllowed` ou scope mapping),
  sinon `/broker/oidc/token` répond 403 « Client not authorized to retrieve tokens ».

Front tenantv3 : aucune variable. Le bouton s'affiche quand `user.mesriStudentEligible` est vrai, c'est-à-dire
quand l'API accepterait l'appel. Activer ou couper la fonctionnalité se fait uniquement par le flag du BO.

## Bac à sable

API Particulier staging est public : aucune habilitation. Faux jetons FC `mesri` et `all_fc` (dépôt
`datagouv/apistration`), ou vrai jeton du bac à sable FC v2 via notre Keycloak avec l'identité de test
Angela Claire Louise DUBOIS (seule identité ayant un cas de test MESR). Test réel du 16/10/2026 : 200 en 1 s.

Le bac à sable FC délivre 5 des 6 scopes (`mesri_admission_etablissement_etudes` manque) ; le staging ne filtre
pas les champs par scope, la production le fera.

## Limites connues

- Couverture MESR incomplète (environ 2 M étudiants sur 3 M) : l'upload manuel reste toujours proposé.
- Aucune colonne de provenance : le nom de fichier `statut-etudiant-franceconnect.pdf` et les `TenantLog`
  `DAS_MESRI_STUDENT_FETCHED` / `DAS_MESRI_STUDENT_FAILED` sont les seules traces.
- Document IA analysera le PDF si `document-ia-professional-analysis` est actif pour l'usager.
- Les fichiers déjà présents sur le document PROFESSIONAL sont conservés, le PDF s'y ajoute.
- Production : DataPass `api-particulier/61626` + modalité FranceConnect (support API Particulier) + scopes
  `mesri_*` à activer sur le FS FranceConnect de prod.
