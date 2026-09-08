# Statut COMPLETED — Opt-in de validation opérateur (MVP)

## 1. Vue d'ensemble

Le module **Opt-in COMPLETED** introduit un nouveau statut de dossier `COMPLETED` : un dossier complet et soumis (déclaration sur l'honneur signée), **utilisable immédiatement par le locataire sans vérification opérateur**. Le locataire peut le partager par téléchargement ZIP des justificatifs filigranés, ainsi que par lien et par mail (itération « partage COMPLETED » : page publique et full PDF au design « dossier complété, non vérifié », cf. §7.2) ; l'espace propriétaire et DossierFacile Connect restent réservés aux dossiers `VALIDATED`.

Objectif : réduire la charge opérateur en laissant, par défaut, les dossiers éligibles hors de la file de traitement — la vérification devient un choix explicite du locataire.

Ce système garantit :
- **Un comportement strictement inchangé hors rollout** : la machine à états (`Tenant.computeStatus()`) n'est pas modifiée ; le passage en `COMPLETED` est une surcouche conditionnée par un feature flag.
- **L'invisibilité totale du statut pour les partenaires** (DFC, api-partner, espace propriétaire) via trois verrous complémentaires (§7).
- **Un seul fait persisté : le choix explicite de l'utilisateur** (`validation_requested`) — l'éligibilité est toujours recalculée, jamais stockée.
- **Un rollback maîtrisé** : action BO explicite qui rebascule tous les dossiers `COMPLETED` en file de traitement.

Périmètre MVP : dossiers **ALONE uniquement**, rollout progressif démarré à 5 % (`only_for_new_user`)

---

## 2. Modèle de données & migrations Liquibase

### 2.1 Champ `validation_requested` sur la table `tenant`
- Nom : `validation_requested` — Type : `boolean` **nullable**, sans défaut.
- Sémantique : capture **le choix explicite de l'utilisateur, et rien d'autre** :
  - `null` = jamais répondu à la question (état par défaut) ;
  - `true` = a demandé une vérification opérateur ;
  - `false` = a explicitement décliné.
- **Invariant : un seul écrivain, le clic de l'utilisateur** (endpoint §6). Ni la signature, ni une bascule partenaire, ni un rollback ne modifient ce champ. Seule exception : la remise à `null` quand le choix perd son objet (changement de type d'application, regroupement BO).
- Migration : `20260806000000-add-completed-optin.xml` (changeSet 1).

### 2.2 Valeur d'enum `TenantFileStatus.COMPLETED`
- `COMPLETED("complété")` — le label FR est affiché dans le BO.
- Persistée dans `tenant.status` comme les autres valeurs. La file BO (`ranked_tenant`, `findMyNextApplication`, compteurs) filtrant sur `status = 'TO_PROCESS'`, **un dossier COMPLETED en sort par construction, sans aucune modification de ces requêtes**.

### 2.3 Feature flag `tenant_completed_optin`
- Inséré inactif, `rollout_pct = 0`, `only_for_new_user = true` (seuls les comptes créés après l'activation entrent dans le rollout).
- Migration : `20260806000000-add-completed-optin.xml` (changeSet 2). Pilotage ensuite via l'écran BO `/bo/feature-flags` (rôle ADMIN).

---

## 3. Règles d'éligibilité & formule du statut

### 3.1 Formule
```
COMPLETED  ⟺  dossier complet + déclaration sur l'honneur signée
              + éligible (§3.2)
              + validation_requested ≠ true
              + le dossier ENTRE en file (statut persisté ≠ TO_PROCESS)   — §3.3
```

### 3.3 Invariant : un dossier en file n'en sort que sur décision explicite
L'éligibilité peut changer **après** la soumission (hausse du `rollout_pct` : `ROLLOUT_INCREASED` rebascule d'un coup toutes les assignations `HASH` dont le bucket passe sous le nouveau seuil ; suppression d'une ligne `tenant_userapi` ; dissociation d'une coloc). Sans garde, un dossier `TO_PROCESS` devenu éligible quittait silencieusement la file au premier recalcul de statut — y compris lors d'une action opérateur dans le BO.

### 3.2 Éligibilité (`CompletedEligibilityService`, common-library)
Toujours **calculée à la volée, jamais stockée** (elle dépend d'états qui changent à tout moment). Conditions, évaluées de la moins chère à la plus chère :
1. `application_type = ALONE` ;
2. **aucune** ligne `tenant_userapi` — règle stricte : tout lien partenaire (DFC ou propriétaire via `dfconnect-proprietaire`), même résiduel après suppression de la candidature, désactive l'opt-in ;
3. **en dernier** : `FeatureFlagService.isFeatureEnabledForUser(tenantId, "tenant_completed_optin")` — la première évaluation persiste l'assignation de bucket du user ; en le plaçant en dernier, seuls les candidats réels entrent dans le dénominateur des métriques, et les candidats hors rollout (`enabled = false`) forment le groupe contrôle.

Deux méthodes publiques :
- `isEligibleForOptIn(tenant)` : conditions 1-3 + dossier soumis (statut `TO_PROCESS`, `COMPLETED`, `VALIDATED` ou `DECLINED`). Pilote l'affichage de la question côté front ; **indépendante de `validation_requested`** (la question reste modifiable après réponse). Sur un dossier `VALIDATED`/`DECLINED`, le choix est enregistré **sans effet immédiat sur le statut** : il s'applique à la prochaine re-soumission (UI dédiée à venir ; l'encart actuel ne s'affiche que pour `COMPLETED` et `TO_PROCESS` + demande en cours).
- `canBeCompleted(tenant)` : conditions 1-3 + `validation_requested ≠ true`. Utilisée pour l'attribution du statut `COMPLETED` ; l'appelant garantit que le statut calculé est `TO_PROCESS`.

---

## 4. Composants & architecture

```
            +---------------------------------------------------------------+
            |                    dossierfacile-api-tenant                   |
            |                                                               |
            |  HonorDeclaration.saveStep ──> TenantStatusServiceImpl        |
            |    (mail selon statut résultant)   (opt-in §5.1)               |
            |  TenantController PUT /api/tenant/validation-request (§6)     |
            |  TenantMapper: optInEligible + validationRequested (§6)       |
            |  TenantServiceImpl: verrous partage lien/mail (§7.2)          |
            +-------------------------------+-------------------------------+
                                            |
                                            v
            +---------------------------------------------------------------+
            |                  dossierfacile-common-library                 |
            |                                                               |
            |  CompletedEligibilityService / Impl (§3)                      |
            |  PartnerCallBackServiceImpl.registerTenant (bascule §7.1)     |
            |  ApartmentSharing.getStatus() : agrégat COMPLETED (§5.3)      |
            |  PartnerVisibleStatus.mask : filet + log.error (§7.3)         |
            |  MailCommonService.sendEmailCompletedSwitchedToProcessing     |
            +-------------------------------+-------------------------------+
                                            ^
                                            |
            +-------------------------------+-------------------------------+
            |                       dossierfacile-bo                        |
            |                                                               |
            |  TenantService.updateTenantStatus : opt-in (§5.2)           |
            |  TenantService.regroupTenant : blocage + purge (§8)           |
            |  BOFeatureFlagsController /completed-rollback (§9)            |
            +---------------------------------------------------------------+
```

---

## 5. Transitions de statut

### 5.1 Entrée : passage en COMPLETED à la soumission (api-tenant)
`TenantStatusServiceImpl.updateTenantStatus()` — appelé par tous les `SaveStep`, dont la déclaration sur l'honneur — applique `CompletedDossierService.toCompletedIfEligible()` : si `computeStatus()` retourne `TO_PROCESS`, que le statut persisté n'est **pas** déjà `TO_PROCESS` (§3.3) **et** `canBeCompleted(tenant)`, le statut persisté devient `COMPLETED`. `Tenant.computeStatus()` est inchangé. Aucun webhook partenaire n'est émis (un dossier passé en `COMPLETED` n'a par définition aucun partenaire lié).

### 5.2 Même règle côté BO
`fr.gouv.bo.service.TenantService.updateTenantStatus()` (re-synchronisation après suppression/modification de document par un opérateur) applique la même règle via `CompletedDossierService.toCompletedIfEligible()` — la logique n'existe qu'à un seul endroit. Grâce à l'invariant §3.3, une action opérateur sur un dossier `TO_PROCESS` ne peut jamais le sortir de la file ; seul un dossier `COMPLETED` consulté par recherche peut être recalculé vers `INCOMPLETE`/`COMPLETED`.

### 5.3 Agrégat coloc
`ApartmentSharing.getStatus()` gère `COMPLETED` explicitement (entre `TO_PROCESS` et `VALIDATED`), la structure existante de la méthode restant inchangée. Sans ce cas, un dossier ALONE `COMPLETED` serait tombé dans le `return VALIDATED` final — exposant tokens de partage, full PDF et mails propriétaire « dossier validé ».

### 5.4 Sorties
| Cause | Mécanisme | `validation_requested` |
|---|---|---|
| Le locataire demande une vérification | endpoint §6 → `TO_PROCESS`, `last_update_date = now` (position en file = moment du choix) | `true` (son choix) |
| Le locataire annule sa demande (sens inverse : `TO_PROCESS → COMPLETED`) | endpoint §6 → `switchToCompleted()` (§3.3), seule sortie de file à l'initiative du locataire | `false` (son choix) |
| Connexion à un partenaire ou candidature propriétaire | bascule automatique §7.1 | inchangé |
| Rollback | action BO §9 | inchangé |
| Modification du dossier | `computeStatus()` → `INCOMPLETE`, puis retour possible en `COMPLETED` à la re-signature | inchangé |
| Changement de type ALONE → COUPLE/GROUP | purge + recalcul (§8) | remis à `null` |

Toute sortie de `COMPLETED` invalide le full PDF (design « non vérifié », §7.2) via `resetDossierPdfGenerated` ; les liens de partage existants survivent.

---

## 6. Choix de l'utilisateur & exposition front

- **`PUT /api/tenant/validation-request`** (scope `dossier`), body `{"validationRequested": true|false}` :
  - refuse (`409`) si `isEligibleForOptIn` est faux ;
  - persiste le choix, positionne `last_update_date = now`, journalise (`VALIDATION_REQUESTED` / `VALIDATION_DECLINED`), puis recalcule le statut — sauf pour l'annulation d'un dossier `TO_PROCESS`, qui passe par `switchToCompleted()` (§3.3) ;
  - si le dossier passe effectivement `COMPLETED → TO_PROCESS` (réponse « oui »), envoie le **template Brevo 56 existant** (« dossier complet, en attente de vérification ») — même situation qu'une soumission classique, aucun nouveau template ;
  - accepté aussi sur un dossier `VALIDATED` ou `DECLINED` : le recalcul de statut est alors un no-op (`computeStatus()` retourne le même statut), le choix est simplement enregistré pour la prochaine re-soumission.
- **`TenantModel`** (profil locataire) expose :
  - `validationRequested` (`Boolean`, absent du JSON si `null` — jamais répondu) ;
  - `optInEligible` (`boolean` primitif, toujours sérialisé) : pilote l'affichage de l'encart « Voulez-vous une validation opérateur ? » sur le tableau de bord. Vrai aussi pour les dossiers `VALIDATED`/`DECLINED` éligibles.
- Les modèles partenaires (DFC, api-partner, api-owner) n'exposent **aucun** de ces champs.

---

## 7. Invisibilité partenaires — les trois verrous

Un dossier `COMPLETED` ne doit jamais être vu d'un partenaire (DFC, api-partner) ni d'un propriétaire. Trois mécanismes complémentaires (§7.1, §7.3, §7.4) — le §7.2 décrit le régime des liens de partage, qui ne créent aucun lien partenaire :

### 7.1 Bascule automatique à la liaison partenaire
`PartnerCallBackServiceImpl.registerTenant()` — point de passage unique de toute création de lien `tenant_userapi` (connexion DFC, candidature propriétaire via `dfconnect-proprietaire`, propagation aux colocataires) — délègue à **`CompletedDossierService.switchBackToProcessing()`** (logique de bascule unique, partagée avec le rollback) : si le tenant est `COMPLETED`, il repasse `TO_PROCESS` **avant** l'envoi du callback (le webhook `CREATED_ACCOUNT` part donc avec un statut connu des partenaires), avec `last_update_date = now`, log `COMPLETED_SWITCHED_TO_PROCESS` et mail au locataire (template `brevo.template.id.completed.switched.to.processing`) après commit.

### 7.2 Liens de partage & full PDF (itération « partage COMPLETED »)
Le partage par lien/mail est ouvert aux dossiers `VALIDATED` **et** `COMPLETED` (`TenantFileStatus.isCompletedOrValidated()`, contrôlé par `TenantServiceImpl.requireCompletedOrValidatedDossier()` et `ApartmentSharingLinkController`, `409` sinon). Les liens LINK/MAIL peuvent donc coexister avec un dossier `COMPLETED` ; ils ne créent aucune ligne `tenant_userapi` et n'entament pas l'invisibilité partenaires. La page publique d'un dossier `COMPLETED` affiche un design « dossier complété, non vérifié par un agent » distinct du rendu `VALIDATED`. Le full PDF `COMPLETED` est un rendu neutre : uniquement les pages de justificatifs (pas de page de garde ni de « mot du locataire ») et sans les logos République Française / DossierFacile dans l'en-tête des pages ; le full PDF `VALIDATED` reste le rendu historique inchangé. La génération du full PDF exige un dossier `VALIDATED` ou `COMPLETED` avec tous les watermarks présents (`countTenantsBlockingFullPdfGeneration`, `417` sinon). À toute sortie de `COMPLETED` (opt-in « oui » §6, bascule partenaire §7.1, rollback §9), les liens **survivent** (la page publique bascule sur le rendu du nouveau statut) mais le full PDF est **invalidé** (`resetDossierPdfGenerated`) — il sera régénéré paresseusement au design du statut courant ; `complete()` (pdf-generator) jette par ailleurs tout fichier généré pendant une invalidation concurrente. Le ZIP (`GET /api/application/zip`) reste accessible quel que soit le statut.

### 7.3 Filet défensif dans les mappers
`PartnerVisibleStatus.mask(status, source)` (common-library) : convertit `COMPLETED → TO_PROCESS` et émet un **`log.error`** — « *Defensive status masking triggered in {source}…* ». Ce cas ne doit jamais se produire : toute occurrence dans ELK signale un invariant cassé, à investiguer (les dossiers concernés se retrouvent via `status = 'COMPLETED' AND EXISTS tenant_userapi`). Branché dans six mappers :
- conditionnel au contexte partenaire (`userApi != null`) là où le mapper sert aussi le locataire : `ApplicationFullMapper` (webhooks, api-partner), `TenantMapper` (profil + DFC) ;
- inconditionnel dans le module api-owner, dont tous les lecteurs sont externes : `PropertyMapper`, `OwnerPropertyMapper`, `ApartmentSharingModelMapper`, `OwnerMapper`.

### 7.4 Règle d'éligibilité stricte
Toute ligne `tenant_userapi`, même résiduelle, rend le dossier inéligible (§3.2) : un dossier lié à un partenaire ne peut pas *devenir* `COMPLETED`, la bascule 7.1 garantissant qu'il ne peut pas le *rester*.

---

## 8. Protections BO & colocation

- **Changement de type côté funnel** (`Application.saveStep`) : quand le type d'application change, `validation_requested` est remis à `null` sur tous les tenants retenus et leurs statuts sont recalculés — un dossier `COMPLETED` ne survit pas à un passage en COUPLE/GROUP.
- **Regroupement BO** (`regroupTenant`) : refusé (`IllegalStateException`) si le tenant ou le dossier cible est `COMPLETED` ; le choix du tenant regroupé est purgé.
- **Dissociation** : aucune protection nécessaire — un dossier `COMPLETED` est forcément ALONE, et la dissociation révoque déjà tous les accès partenaires du tenant dissocié.
- **File opérateur** : garde-fou `processFile` existant (statut `TO_PROCESS` exigé) inchangé ; un dossier `COMPLETED` est consultable dans le BO (libellé « complété ») mais jamais distribué.

---

## 9. Rollback

**Rollback total uniquement**, en deux temps, volontairement manuel :
1. **Désactiver le flag (ou le passer à 0 %)** sur `/bo/feature-flags` → les nouvelles soumissions repartent en `TO_PROCESS` ; les dossiers `COMPLETED` existants sont conservés.
2. Action **« Rebasculer les dossiers COMPLETED »** (même écran, `POST /bo/feature-flags/completed-rollback`, rôle ADMIN, confirmation) → tous les `COMPLETED` passent `TO_PROCESS` avec `last_update_date = now` (fin de file) et log `COMPLETED_SWITCHED_TO_PROCESS`. **Pas de mail dans ce cas** : le template de bascule (174) mentionne le partenaire connecté, sans objet ici — un template dédié pourra être ajouté si un rollback devait réellement être exécuté.

Garde-fous :
- l'action est **refusée tant que le flag est actif avec un rollout > 0 %** (bouton masqué dans l'écran + `IllegalStateException` côté service) — une baisse partielle du rollout ne doit pas rebasculer des dossiers encore couverts ;
- **une transaction par dossier** (via `CompletedDossierService.switchBackToProcessing`) : l'échec d'un dossier n'annule pas le lot, il est journalisé et le traitement continue.

Le choix (`validation_requested`) n'est pas modifié : si le flag est réactivé, les dossiers redeviennent éligibles avec leur historique intact.

---

## 10. Mails

| Situation | Template | Propriété |
|---|---|---|
| Soumission, dossier `TO_PROCESS` (hors rollout / inéligible) | **56 — inchangé** (« en attente de vérification ») | `brevo.template.id.account.completed` |
| Soumission, dossier `COMPLETED` | 173 (« votre dossier est complété », relaie la question de l'encart) | `brevo.template.id.account.completed.optin` |
| Réponse « oui » à l'encart (`COMPLETED → TO_PROCESS`) | **56 — réutilisé** | — |
| Bascule partenaire (§7.1) | 174 (« votre dossier va être vérifié par notre équipe », paramètre `PARTENAIRE`) | `brevo.template.id.completed.switched.to.processing` |
| Rollback (§9) | aucun mail (template dédié à créer si besoin) | — |
| Validation après opt-out | mails de validation existants, inchangés | — |

Le branchement du mail de soumission se fait sur le **statut résultant** (dans `HonorDeclaration.saveStep`), jamais sur le feature flag.

---

## 11. Journalisation & observabilité

- Nouveaux `LogType` dans `tenant_log` : `VALIDATION_REQUESTED`, `VALIDATION_DECLINED` (choix utilisateur), `COMPLETED_SWITCHED_TO_PROCESS` (toute bascule automatique ou rollback).
- Métriques du MVP par simple SQL : répartition de `validation_requested` (`null` = jamais répondu / `false` = complété par défaut ou décliné / `true` = vérification demandée) sur la cohorte assignée (`user_feature_assignment`, flag `tenant_completed_optin`), croisée avec les logs.
- Point de surveillance ELK post-activation : le message « `Defensive status masking triggered` » (§7.3) — zéro occurrence attendue.
- `callback_log.tenant_status` ne doit jamais contenir `COMPLETED`.

## 12. Non-impacts vérifiés

- **Relances mails** (api-tenant `ScheduledTasksServiceImpl`, task-scheduler `TenantWarningTask`) : aucune ne filtre sur le statut `TO_PROCESS` → comportement identique pour un dossier `COMPLETED`.
- **Archivage / suppression** : filtres sur `last_login_date` / `warnings`, insensibles au nouveau statut.
- **Auto-validation Visale** : le garde-fou existant (`status = TO_PROCESS` exigé) fait que le bot ignore les dossiers `COMPLETED`. Un opt-out Visale-éligible sera auto-validé gratuitement. Évolution possible post-MVP : upgrade silencieux `COMPLETED → VALIDATED` par le bot.
- **File BO** (`ranked_tenant`, `findMyNextApplication`, compteurs) : requêtes inchangées, exclusion par construction.

---

## 13. Scénarios de test manuel

Préparation : flag activé en préprod, `rollout_pct` à 100 % (cohorte test) ou 0 % (contrôle) ; comptes créés **après** l'activation (`only_for_new_user`). Vérifications SQL : `tenant.status`, `tenant.validation_requested`, `user_feature_assignment`, `tenant_log`, `callback_log`.

### A. Hors rollout — non-régression stricte

| # | Scénario | Attendu |
|---|---|---|
| A1 | Funnel ALONE complet → signature déclaration sur l'honneur | Statut `TO_PROCESS`, mail template 56, badge « en cours de traitement », **pas d'encart** opt-in |
| A2 | Dossier A1 dans le BO | Présent dans la file (`ranked_tenant`), compteurs inchangés, traitable (validation → VALIDATED + mail ; refus → DECLINED + mail) |
| A3 | Partage pendant TO_PROCESS | `/partages` sans formulaire ; bloc « documents non vérifiés » + ZIP OK ; après VALIDATED : création lien/mail OK |
| A4 | COUPLE : signature | Les deux passent TO_PROCESS, mail au signataire seul (comportement actuel), pas d'encart |
| A5 | Compte créé via partenaire DFC → funnel → signature | TO_PROCESS, webhook `CREATED_ACCOUNT`, pas d'encart |
| A6 | Vérif SQL `user_feature_assignment` | Le user A1 (candidat réel) est assigné `enabled=false` (groupe contrôle) ; les users A4/A5 ne sont **pas** assignés (conditions 1-2 échouent avant le flag) |

### B. Dans le rollout (rollout 100 % en préprod)

| # | Scénario | Attendu |
|---|---|---|
| B1 | Funnel ALONE complet → signature | Statut `COMPLETED`, badge « Complété », mail template 173, encart affiché sans réponse (`validation_requested = null`) |
| B2 | Dossier B1 côté BO | Absent de la file et des compteurs ; trouvable par recherche directe, libellé « complété » ; `processFile` impossible |
| B3 | Partage B1 | ZIP OK ; création lien/mail et lien par défaut **OK** ; page publique au design « dossier complété, non vérifié » (badges info, documents consultables) ; full PDF généré paresseusement au design COMPLETED |
| B3bis | Lien créé pendant B1 puis sortie de COMPLETED (opt-in « oui » ou liaison partenaire) | Le lien reste actif et la page publique bascule sur le rendu TO_PROCESS ; le full PDF est invalidé (409/417) puis régénéré au design du nouveau statut |
| B4 | Encart : répondre « oui » | `validation_requested=true`, statut TO_PROCESS, entre en file (position = maintenant), badge « en cours », log `VALIDATION_REQUESTED`, mail template 56 reçu |
| B5 | B4 puis annuler | Retour `COMPLETED`, `validation_requested=false`, sort de la file (`switchToCompleted`) |
| B6 | B1 → connexion à un partenaire DFC | Bascule immédiate TO_PROCESS, `validation_requested` reste `null`, mail de bascule, webhook `CREATED_ACCOUNT` avec `status=TO_PROCESS` (payload + `callback_log` : jamais COMPLETED), encart disparu, dossier en file |
| B7 | B1 → candidature propriétaire (`/candidater/:token`) | Même bascule que B6 ; écran de confirmation owner = wording TO_PROCESS standard ; mail owner « candidat non validé », **pas** le « validé » |
| B8 | B1 → modification d'un document | INCOMPLETE → re-signature → re-COMPLETED (choix inchangé, pas de nouvelle question) |
| B9 | B4 → validé par un opérateur → modification d'un document | Le choix `validation_requested=true` de B4 persiste → re-soumission en TO_PROCESS, **encart présent** (« annuler ma demande ») ; l'annulation bascule le dossier en COMPLETED. Un dossier examiné (validé ou refusé) dont le choix est `null`/`false` re-soumet **directement en COMPLETED** |
| B10 | B1 → ajout d'un conjoint (ALONE → COUPLE) | `validation_requested` purgé, dossier INCOMPLETE ; après signature des deux → TO_PROCESS (plus ALONE), pas d'encart |
| B11 | Compte rollout mais connecté DFC avant soumission | Signature → TO_PROCESS direct, mail 56 classique, pas d'encart |
| B12 | Vérif exposition B1 | Profil : `optInEligible=true`, `validationRequested` absent ; `apartmentSharing.status` ≠ VALIDATED (tokens de partage absents du JSON) |
| B13 | BO : regroupement impliquant B1 | Bloqué avec message |
| B14 | Dossier A1 (hors rollout, en file) → passage du rollout à 100 % → suppression d'un fichier par un opérateur dans le BO, ou ajout d'un document par le locataire | Reste `TO_PROCESS` et en file ; le verdict opérateur fonctionne (§3.3). Après refus puis correction : re-soumission directement en `COMPLETED` (B9) |

### C. Transverses / rollback

| # | Scénario | Attendu |
|---|---|---|
| C0 | Rollout 25 → 50 % avec des dossiers ALONE en file | `ROLLOUT_INCREASED` sur les assignations, **aucun changement de statut** : les dossiers en file y restent (§3.3), pas d'encart (affiché seulement pour `COMPLETED` ou demande en cours) |
| C1 | Rollout 100 → 0 % | Nouvelles soumissions → TO_PROCESS ; dossiers COMPLETED existants **inchangés** tant que l'action de rollback n'est pas lancée |
| C2 | Action « Rebasculer les dossiers COMPLETED » | Tous → TO_PROCESS, `last_update_date=now` (fin de file), `validation_requested` intact, mail de bascule, logs `COMPLETED_SWITCHED_TO_PROCESS` |
| C3 | Métriques | Répartition `validation_requested` sur la cohorte assignée ; comptage des logs |
| C4 | Partenaires | `callback_log.tenant_status` : aucune ligne COMPLETED ; API DFC/api-partner : jamais COMPLETED dans les payloads |
| C5 | ELK | Aucune occurrence de « Defensive status masking triggered » |

---

## 14. Limites du MVP & évolutions envisagées

- **Colocs/couples exclus** (76-77 % du volume est ALONE) : l'extension passera par l'agrégat `ApartmentSharing.getStatus()` déjà en place (« pire statut gagne » : un seul tenant sans opt-in maintient le dossier global en `TO_PROCESS`).
- ~~**Partage ZIP uniquement**~~ : levé par l'itération « partage COMPLETED » (§7.2) — liens LINK/MAIL, page publique et full PDF au design dédié.
- **Plafond journalier** : hors MVP ; réutilisera le statut `COMPLETED` comme second chemin d'entrée.
- **Upgrade auto-validation** : laisser le bot Visale valider silencieusement les dossiers `COMPLETED` éligibles (coût opérateur nul, bénéfice usager).
- Le **frontend** (badge, bloc ZIP, encart de choix — monorepo Dossier-Facile-Frontend) fait l'objet d'une PR séparée consommant `optInEligible` / `validationRequested` / `POST /api/tenant/validation-request`.
