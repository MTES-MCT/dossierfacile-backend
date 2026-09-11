# Ouverture du statut COMPLETED aux partenaires l'ayant intégré (flag `partner_completed_optin`)

## 1. Vue d'ensemble

Le statut `COMPLETED` (cf. [completed-optin.md](completed-optin.md)) était invisible des partenaires : toute liaison partenaire renvoyait le dossier en file opérateur (`TO_PROCESS`), donc **toute liaison partenaire était un bypass du tirage au sort** ([tenant-lottery.md](tenant-lottery.md)) et un coût opérateur non plafonné.

Ce module ouvre `COMPLETED` **partenaire par partenaire** : un partenaire **ayant intégré le statut `COMPLETED`** voit le statut `COMPLETED` dans ses payloads, reçoit le webhook `COMPLETED_ACCOUNT`, et la liaison d'un dossier `COMPLETED` à ce partenaire ne le renvoie plus en file.

Garanties :
- **Flag inactif = comportement antérieur exact** : aucun partenaire n'a intégré `COMPLETED`, les trois verrous de l'opt-in (completed-optin.md §7) s'appliquent à tous.
- Le partenaire n'est **pas notifié** du passage `COMPLETED → TO_PROCESS`. : depuis l'itération « partage en TO_PROCESS », les deux statuts sont des dossiers *non vérifiés* au rendu identique (page publique, full PDF, URLs). Le partenaire n'a donc **pas** à être notifié du passage `COMPLETED → TO_PROCESS`.
- **Un partenaire n'ayant pas intégré `COMPLETED` ne le voit jamais** : verrous conditionnels + filets défensifs (§4).
- **L'espace propriétaire (`dfconnect-proprietaire`) n'est pas considéré comme ayant intégré `COMPLETED`** (garde-fou en lecture et en saisie).
- **Pas de rollback par partenaire** dans cette version (§9).

Vocabulaire : un partenaire **ayant intégré le statut COMPLETED** (*partner opted in* dans le code) est un partenaire listé dans le flag actif `partner_completed_optin`. Les méthodes en découlent : `FeatureFlagService.isPartnerOptedIn(key, userApi)`, `OperatorReviewPolicy.isPartnerOptedIn(userApi)`.

---

## 2. Modèle de données

Migration : `20260910000000-add-partner-completed-optin-flag.xml`.

- **`feature_flag.opted_in_partners`** (`TEXT`, nullable) : liste de **client ids Keycloak** des partenaires (`dfconnect-xxx`, stockés dans `user_api.name`, champ « clientId » de la fiche partenaire BO ; `name2` est le libellé d'affichage) séparés par des virgules. Null ou vide = aucun partenaire. Parsing dans l'entité (`FeatureFlag.getOptedInPartnerNames()` : trim, doublons et vides ignorés, ordre conservé).
- **Flag `partner_completed_optin`**, inséré inactif, `only_for_new_user = false`, `rollout_pct = 100` : flag **global** (comme `tenant_lottery`), `rollout_pct` / `only_for_new_user` ignorés.


---

## 3. Pilotage BO

Écran `/bo/feature-flags` (rôle ADMIN) :
- le flag est marqué **Global** (pas de rollout) et affiche ses **partenaires opt-in** en badges ;
- bouton **« Partenaires »** → modale avec la liste éditable (`user_api.name`, séparés par des virgules) et la liste des partenaires connus en aide à la saisie ;
- `POST /bo/feature-flags/opted-in-partners` (`key`, `value`) : refuse une clé qui n'est pas un flag partenaire, refuse `dfconnect-proprietaire`, refuse toute liste contenant un nom inconnu (`user_api.name`) — **rien n'est enregistré** dans ces cas, message d'erreur listant les noms inconnus ; succès → message de confirmation, `log.info` ancien → nouveau.

Procédure d'ouverture d'un partenaire : recette de son intégration en préprod (§7), ajout à la liste, puis activation du flag s'il ne l'est pas encore. **Retirer un nom n'agit que sur les nouvelles soumissions et liaisons** (§9).

---

## 4. Verrous conditionnels

Point d'entrée unique : `OperatorReviewPolicy.isPartnerOptedIn(userApi)` → `FeatureFlagService.isPartnerOptedIn("partner_completed_optin", userApi)` (faux si `userApi` nul, nom blanc ou `dfconnect-proprietaire` ; sinon flag actif **et** nom listé, comparaison exacte après trim).

| Verrou (completed-optin.md §7) | Avant | Après |
|---|---|---|
| Éligibilité (`OperatorReviewPolicyImpl.supportsCompletedStatus`, règle 2) | aucune ligne `tenant_userapi` | **tous** les liens `tenant_userapi` (`findAllByTenant`) pointent vers un partenaire ayant intégré COMPLETED ; un seul partenaire n'ayant pas intégré COMPLETED force `TO_PROCESS`. Le flag opt-in locataire reste évalué en dernier (assignation de bucket) |
| Bascule à la liaison (`PartnerCallBackServiceImpl.registerTenant`) | toujours `switchBackToProcessing` | seulement si le partenaire n'a pas intégré `COMPLETED` |
| Masque (`MasksCompletedStatusForPartner`, classe de base de `ApplicationFullMapper` et `TenantMapper`) | masque si `userApi != null` | masque si `userApi != null` **et** partenaire n'ayant pas intégré COMPLETED ; le statut est testé avant le flag (une lecture du flag seulement pour un `COMPLETED`). `TenantMapper` cache aussi `optInEligible` / `validationRequested` en contexte partenaire |
| Filet webhook (`PartnerCallBackServiceImpl.getWebhookDTO`) | — | `COMPLETED_ACCOUNT` vers un partenaire n'ayant pas intégré COMPLETED → `CREATED_ACCOUNT` + `log.error` « *Defensive callback type downgrade* » |

Les mappers propriétaires (`MasksCompletedStatusForOwner`, api-owner) restent inconditionnels.

`DfcTenantController.profilePartner` recharge le tenant après la liaison avant de le mapper : la liaison peut avoir basculé le dossier, et le graphe chargé avant elle était périmé (cause de l'unique déclenchement du masque observé en prod, 10/09/2026).

---

## 5. Webhooks

Règle du contrat : **à chaque entrée dans un état, un événement nommé d'après cet état, avec le payload complet** : `CREATED_ACCOUNT` (`TO_PROCESS`), **`COMPLETED_ACCOUNT`** (`COMPLETED`, nouveau), `VERIFIED_ACCOUNT` (`VALIDATED`), `DENIED_ACCOUNT` (`DECLINED`). `PartnerCallBackType.forTenantStatus(status)` porte cette règle pour les renvois (liaison, BO, scheduler).

| Transition | Déclencheur | Avant | Partenaire n'ayant pas intégré COMPLETED | Partenaire ayant intégré COMPLETED |
|---|---|---|---|---|
| `INCOMPLETE → TO_PROCESS` | Signature (hors périmètre opt-in, ou lié à un partenaire n'ayant pas intégré COMPLETED) | `CREATED_ACCOUNT` | `CREATED_ACCOUNT` | `CREATED_ACCOUNT` |
| `INCOMPLETE → COMPLETED` | Signature, dossier dans le périmètre | impossible | impossible | **`COMPLETED_ACCOUNT`** |
| `TO_PROCESS → COMPLETED` | Le locataire retire sa demande de vérification | impossible | impossible | **`COMPLETED_ACCOUNT`** |
| `COMPLETED → TO_PROCESS` | Opt-in « oui » hors loterie, ticket tiré, liaison à un partenaire n'ayant pas intégré COMPLETED, rollback | impossible | `CREATED_ACCOUNT` au partenaire qui se lie (comme avant) | **aucun** |
| Liaison d'un dossier `COMPLETED` | `registerTenant` | bascule puis `CREATED_ACCOUNT` | bascule puis `CREATED_ACCOUNT` | pas de bascule, **`COMPLETED_ACCOUNT`** |
| Liaison d'un dossier `TO_PROCESS` / `VALIDATED` | `registerTenant` | `CREATED_ACCOUNT` / `VERIFIED_ACCOUNT` | inchangé | inchangé |
| `COMPLETED → INCOMPLETE` | Suppression d'un document | — | — | aucun (comme `TO_PROCESS → INCOMPLETE`) |
| Renvoi manuel BO, scheduler PDF en échec | `BOTenantController`, `DocumentTask` | `CREATED_ACCOUNT` / `VERIFIED_ACCOUNT` | inchangé (filet : jamais `COMPLETED_ACCOUNT`) | **`COMPLETED_ACCOUNT`** si `COMPLETED` |

Émetteurs : `TenantStatusServiceImpl.updateTenantStatus` (entrée en `COMPLETED` depuis `INCOMPLETE` ou `TO_PROCESS` ; **rien** sur `COMPLETED → TO_PROCESS`, ni sur `VALIDATED/DECLINED → TO_PROCESS`, comportement historique), `PartnerCallBackServiceImpl.sendCallbackIfEligible` (liaison, tout statut soumis). `CompletedDossierService.switchBackToProcessing` et le tirage n'émettent rien.

---

## 6. Payload

- `status` (niveau dossier et niveau locataire) vaut `COMPLETED` pour un partenaire ayant intégré COMPLETED. `callback_log.tenant_status` le contient donc pour ces partenaires.
- `dossierUrl` / `dossierPdfUrl` sont renseignés dès qu'un dossier est **soumis** (`TO_PROCESS`, `COMPLETED`, `VALIDATED`) pour tout partenaire disposant d'un lien `PARTNER` full data — itération « partage en TO_PROCESS », indépendante de l'opt-in.
- Jamais exposés à un partenaire : `optInEligible` (`false`), `validationRequested` (absent), `lotteryStatus`, `nextEligibleDate`.
- Hors `status` et ces champs, le contenu est identique entre `TO_PROCESS` et `COMPLETED`.

### Note contrat API (à transmettre aux partenaires avant ouverture)

> Nouvelle valeur `COMPLETED` de `status` (dossier et locataire) : dossier complet, soumis, **non vérifié par un agent** ; documents, liens et full PDF exploitables comme en `TO_PROCESS`. Nouveau `partnerCallBackType` **`COMPLETED_ACCOUNT`**, émis à chaque entrée en `COMPLETED` ; il peut être le **premier** événement reçu pour un dossier, à la place de `CREATED_ACCOUNT`. **Aucun événement** lors du passage `COMPLETED → TO_PROCESS` : le dossier est identique pour vous ; la vérification aboutit ensuite à `VERIFIED_ACCOUNT` ou `DENIED_ACCOUNT`. `dossierUrl` / `dossierPdfUrl` sont désormais renseignés dès `TO_PROCESS`. L'ouverture se fait partenaire par partenaire, après recette.

---

## 7. Scénarios de test manuel (préprod)

Préparation : flag `tenant_completed_optin` actif à 100 %, `tenant_lottery` OFF puis ON, deux partenaires DFC de test `P_open` et `P_closed`, `partner_completed_optin` actif avec `P_open` listé (sauf S1-S2). Vérifications : `tenant.status`, `tenant_userapi`, `tenant_log`, `callback_log` (`partner_id`, `tenant_status`, payload), ELK.

| # | Scénario | Attendu |
|---|---|---|
| S1 | Flag `partner_completed_optin` **inactif**, `P_open` listé : liaison DFC d'un dossier COMPLETED | Bascule TO_PROCESS, mail 174, `CREATED_ACCOUNT` avec `status=TO_PROCESS` |
| S2 | Idem, flag actif mais liste vide | Idem S1 |
| S3 | Flag actif, compte lié à `P_open` avant soumission → signature | `COMPLETED`, `COMPLETED_ACCOUNT` avec `status=COMPLETED` aux deux niveaux, `dossierUrl` / `dossierPdfUrl` renseignés ; GET api-partner : `optInEligible=false`, pas de `validationRequested` ; encart opt-in visible côté locataire |
| S4 | S3 → opt-in « oui » (loterie OFF) | TO_PROCESS, **aucune** nouvelle ligne `callback_log` ; annulation → COMPLETED, nouveau `COMPLETED_ACCOUNT` |
| S5 | S3 → loterie ON, ticket tiré | TO_PROCESS sans webhook ; validation opérateur → `VERIFIED_ACCOUNT` |
| S6 | S3 → liaison à `P_closed` | Bascule TO_PROCESS, mail 174 nommant `P_closed`, `CREATED_ACCOUNT` à `P_closed` seulement ; `optInEligible=false` côté locataire (plus éligible) ; re-soumission ultérieure → TO_PROCESS |
| S7 | Dossier COMPLETED sans partenaire → liaison à `P_open` | Pas de bascule, `COMPLETED_ACCOUNT`, `callback_log.tenant_status=COMPLETED` |
| S8 | BO : renvoi manuel d'un COMPLETED vers `P_open` / vers `P_closed` | `COMPLETED_ACCOUNT` / `CREATED_ACCOUNT` + `log.error` « Defensive callback type downgrade » (cas anormal, ne se produit que si `P_closed` est lié à un COMPLETED) |
| S9 | BO : saisie de `dfconnect-proprietaire`, d'un nom inconnu, d'une liste avec espaces | Refus avec message / refus listant l'inconnu / liste normalisée enregistrée |
| S10 | DFC `GET /dfc/tenant/profile` d'un COMPLETED avec `P_closed` | Réponse `TO_PROCESS` (bascule) **sans** `log.error` de masquage |

---

## 8. Observabilité

- ELK, zéro occurrence attendue : « `Defensive status masking triggered` » (mappers) et « `Defensive callback type downgrade` » (webhook).
- Invariant SQL (zéro ligne attendue) : dossiers COMPLETED liés à un partenaire n'ayant pas intégré COMPLETED :
```sql
SELECT t.id, ua.name
FROM tenant t
JOIN tenant_userapi tua ON tua.tenant_id = t.id
JOIN user_api ua ON ua.id = tua.userapi_id
WHERE t.status = 'COMPLETED'
  AND ua.name <> ALL (string_to_array(
        (SELECT COALESCE(opted_in_partners, '') FROM feature_flag WHERE key = 'partner_completed_optin'), ','));
```
- `callback_log` : `SELECT partner_id, count(*) FROM callback_log WHERE tenant_status = 'COMPLETED' GROUP BY 1` — uniquement des partenaires listés.
- Effet attendu sur la loterie : baisse du `bypass_count` (`lottery_draw`), les liaisons vers des partenaires ayant intégré COMPLETED n'entrant plus en file.

---

## 9. Limites et évolutions

- **Pas de rollback par partenaire** : retirer un nom de la liste (ou désactiver le flag) n'agit que sur les nouvelles soumissions et liaisons. Les dossiers COMPLETED déjà liés à ce partenaire le restent ; ses lectures sont alors masquées avec `log.error` (§4), et l'invariant SQL §8 les liste. Le rollback global `/completed-rollback` (completed-optin.md §9) reste disponible.
- **Propriétaire** hors périmètre (mappers owner inconditionnels, mails « candidat validé / non validé »).
- **Retour en `INCOMPLETE`** non notifié, comme pour `TO_PROCESS` aujourd'hui.
- **COUPLE / GROUP** : suivent l'extension de l'opt-in locataire.
- Séquence de déploiement : tous les modules (common-library, api-tenant, bo, task-scheduler, pdf-generator, api-owner) **avant** toute activation — l'enum `PartnerCallBackType` est partagée, et le partage en TO_PROCESS touche api-tenant et pdf-generator.
