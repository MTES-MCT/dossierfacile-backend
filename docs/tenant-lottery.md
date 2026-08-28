# Tirage au sort quotidien des demandes de vérification (tenant lottery)

## 1. Vue d'ensemble

Le module **tenant lottery** plafonne le coût opérateur de la vérification opt-in (cf. [completed-optin.md](completed-optin.md)) : le clic « demander une vérification » n'envoie plus le dossier directement en `TO_PROCESS` — il enregistre une **candidature** pour un **tirage au sort quotidien**. Chaque nuit à 00h05 (Europe/Paris), X candidatures sont tirées parmi celles en attente ; les dossiers tirés entrent en file opérateur, les non-tirés subissent un **cooldown de 3 jours** puis doivent **recandidater manuellement** (mail de relance à la fin du cooldown, pas de mail immédiat).

```
X (places du jour J) = processing_capacity.daily_count(J) − bypass mesuré sur le jour civil J−1
```

Le **bypass** = les entrées en `TO_PROCESS` qui ne passent pas par le tirage (liaisons partenaires §7.1 du doc opt-in, dossiers hors rollout opt-in, COUPLE/GROUP). La capacité saisie dans le BO garde sa sémantique de **charge opérateur totale** : tirés + bypass.

Garanties :
- **Le ticket fait foi** : un dossier en file via l'opt-in a toujours un ticket `DRAWN` ; c'est lui — et non plus `validation_requested` — que lisent le calcul de statut (§3) et le comptage du bypass (§7).
- **Un seul tirage par jour** : le job est idempotent et relançable depuis le BO. `lottery.draw.allow-multiple-per-day` (défaut `false`, **préprod uniquement**) lève cette règle pour tester.
- **Kill-switch total** : flag OFF = comportement pré-tirage exact ; la désactivation bascule les candidatures en attente vers la file (§8).
- **Invisibilité partenaires inchangée** : aucun état de loterie exposé aux mappers partenaires ; pas de nouveau statut dans `TenantFileStatus` — l'attente est un état du *ticket*, pas du dossier.

## 2. Modèle de données

Migration : `20260901000000-add-tenant-lottery.xml`

### 2.1 Table `lottery_draw` — une exécution du tirage
| Colonne | Rôle |
|---|---|
| `draw_date` (indexé) | un tirage par jour  |
| `daily_count` | snapshot de la capacité saisie |
| `bypass_count` | bypass mesuré sur J−1 (§7) |
| `available_slots` | places X = daily_count − bypass_count (peut être ≤ 0) |
| `ticket_count` | tickets en jeu (`PENDING` sur dossier `COMPLETED`) |
| `drawn_count` | tickets tirés — au plus min(places, tickets) |

C'est l'enregistrement publiable du calcul et la source des colonnes lecture seule de l'écran BO.

### 2.2 Table `lottery_ticket` — le ticket
`tenant_id` (FK CASCADE), `status`, `created_at`, `lottery_draw_id` (nul pour un flush ou un ticket de reprise), `drawn_at`, `cooldown_until`, `cooldown_notified_at`. Jamais supprimé (audit). Index partiel unique `uq_lottery_ticket_active` : au plus **un ticket actif** par tenant.

### 2.3 Cycle de vie du ticket (`LotteryTicketStatus`)

Un ticket est **actif** en `PENDING` ou `DRAWN` ; `NOT_DRAWN`, `CANCELLED` et `CONSUMED` sont **inactifs** (le tenant peut recandidater — après le cooldown pour `NOT_DRAWN`, immédiatement sinon).

| Transition | Déclencheur | Effet dossier |
|---|---|---|
| — → `PENDING` | clic opt-in « oui » | aucun (reste `COMPLETED`) |
| `PENDING` → `DRAWN` | tiré au sort, ou flush (§8) | `COMPLETED → TO_PROCESS` |
| `PENDING` → `NOT_DRAWN` | non tiré | aucun ; `cooldown_until = J+3` |
| `PENDING` → `CANCELLED` | dossier non `COMPLETED` **au moment du tirage** | aucun ; pas de cooldown |
| `PENDING`/`DRAWN` → `CANCELLED` | annulation locataire, liaison partenaire, changement de type, regroupement BO | si `DRAWN` : retour `COMPLETED` |
| `DRAWN` → `CONSUMED` | verdict opérateur `VALIDATED` **ou** `DECLINED` | toute re-soumission ultérieure retombe en `COMPLETED` et exige une nouvelle candidature |

## 3. Flag & calcul de statut

Flag global on/off `tenant_lottery` (`FeatureFlagService.isFeatureEnabled`, ignore `rollout_pct` / `only_for_new_user`, aucune assignation user) : s'applique à tous les éligibles opt-in, anciens comptes inclus. Inséré inactif.

`CompletedEligibilityService.canBeCompleted` :
- **flag OFF** : comportement historique — `validation_requested = true` bloque `COMPLETED` ;
- **flag ON** : `validation_requested` devient purement déclaratif ; seul un ticket **`DRAWN`** bloque `COMPLETED` (un `PENDING` ne bloque pas : le dossier reste utilisable en attendant le tirage).

**Reprise** (changeSet 4) : les dossiers `TO_PROCESS` avec `validation_requested = true` au déploiement reçoivent un ticket `DRAWN`, pour que « le ticket fait foi » tienne dès le premier jour.

## 4. Composants

```
 common-library
   LotteryTicketService/Impl      cycle de vie d'un ticket (apply / cancel / consume / getPublicStatus)
   LotteryDrawService/Impl        tirage, relance J+3, flush — 1 transaction par ticket (REQUIRES_NEW)
   CompletedEligibilityServiceImpl garde « ticket DRAWN » dans canBeCompleted
   TenantLogCommonService.logQueueEntered  journal des entrées en file (§7)
   MailCommonServiceImpl          mail de fin de cooldown (nouveau template)
 api-tenant
   TenantServiceImpl.updateValidationRequest   branche flag ON/OFF (§5)
   TenantStatusServiceImpl        QUEUE_ENTERED (SUBMISSION) + filet de consommation DECLINED
   TenantMapper (@AfterMapping)   expose lotteryStatus / nextEligibleDate (jamais en contexte partenaire)
   Application.saveStep           annulation du ticket au changement de type
 bo
   TenantService                  QUEUE_ENTERED (BO_RECOMPUTE, BO_REPROCESS), consommation au refus,
                                  annulation au regroupement
   BOFeatureFlagsController       flush à la désactivation du flag
   BOProcessDossierController     colonnes lecture seule (bypass/places/tickets/tirés) + bouton
                                  « Lancer le tirage du jour » (ADMIN), remplacé par le détail du
                                  tirage une fois exécuté ; messages flash succès / déjà exécuté / échec
 task-scheduler
   LotteryDrawTask                cron ${lottery.draw.cron:0 5 0 * * *}, zone Europe/Paris
```

## 5. Endpoint opt-in `PUT /api/tenant/validation-request` (flag ON)

Garde 409 `isEligibleForOptIn`, persistance de `validation_requested` et logs `VALIDATION_REQUESTED` / `VALIDATION_DECLINED` inchangés.
- **`true`** : `409` si cooldown en cours ; sinon `apply()` — idempotent, crée un `PENDING`. Pas de changement de statut, pas de `last_update_date`, pas de mail. La recandidature après cooldown est le même appel.
- **`false`** : `cancelActiveTicket` puis recalcul de statut (un tiré non traité revient en `COMPLETED`).

## 6. Le job de tirage (`LotteryDrawService.executeDrawIfNeeded`)

1. Flag OFF, ou tirage déjà enregistré pour la date → rien.
2. Pas de ligne `processing_capacity` → log.warn, **aucun tirage enregistré** (relançable depuis le BO après saisie).
3. Bypass = comptage §7 sur J−1 ; X places = capacité − bypass ; tickets en jeu = `PENDING` sur dossier `COMPLETED`, en **ordre aléatoire SQL** — l'ordre EST le tirage. Ligne `lottery_draw` créée.
4. **Balayage** : tout `PENDING` dont le dossier n'est pas `COMPLETED` → `CANCELLED`, sans cooldown. Exécuté même si X ≤ 0.
5. **X ≤ 0** → arrêt : rien n'est tiré, **rien ne passe en `NOT_DRAWN`**. Les tickets restent `PENDING` et participent au tirage suivant sans recandidater — un candidat qui n'a perdu aucun tirage ne subit pas de cooldown.
6. Les X premiers → `DRAWN` (une transaction par dossier, échec unitaire loggé) : re-vérification `PENDING` + `COMPLETED` (sinon `CANCELLED`), dossier `TO_PROCESS` + `last_update_date = now`, logs `LOTTERY_DRAWN` + `QUEUE_ENTERED`, invalidation du full PDF, aucun mail. Les autres → `NOT_DRAWN`, `cooldown_until = J+3`, log `LOTTERY_NOT_DRAWN`, aucun mail.
7. `refreshRank()` (sinon la vue matérialisée retarde de 5 min la distribution).

**`notifyCooldownEnded`** (même job, second passage) : `NOT_DRAWN` avec `cooldown_until ≤ today` non notifiés → mail « vous pouvez recandidater » (`brevo.template.id.lottery.cooldown.ended`, **template à créer** ; sans configuration, mail sauté + log.error), envoyé seulement si le dossier est encore `COMPLETED` ; `cooldown_notified_at` posé dans tous les cas.

## 7. Journal des entrées en file (`QUEUE_ENTERED`)

**Chaque** entrée en `TO_PROCESS` émet un `QUEUE_ENTERED` — y compris les tirés. `logDetails` :
- `source` : `SUBMISSION` (api-tenant), `BO_RECOMPUTE`, `BO_REPROCESS`, `PARTNER_LINK` / `COMPLETED_ROLLBACK` (`switchBackToProcessing`), `LOTTERY_DRAW` (tirage ou flush) ;
- `bypass` : `true` si le tenant **n'a pas de ticket `DRAWN`** au moment de l'entrée. Calculé au seul endroit `logQueueEntered` — les appelants n'ont aucune notion de loterie.

Bypass de J−1 = `count(QUEUE_ENTERED where bypass = true)` (`TenantLogRepository.countBypassQueueEntries`, bornée par `log_type` / `creation_date`). Un tiré qui re-soumet avant traitement est donc journalisé mais non compté.

## 8. Désactivation du flag (kill-switch)

`BOFeatureFlagsController.toggle` actif→inactif appelle `flushPendingTicketsToProcessing()` : **un tirage où tout le monde gagne**, sans ligne `lottery_draw` — chaque `PENDING` passe par `drawTicket(ticket, null)` : dossier `COMPLETED` → `DRAWN` + `TO_PROCESS`, sinon `CANCELLED`. Les `DRAWN` suivent leur vie normale ; les cooldowns ne reçoivent plus de mail J+3. Le clic opt-in redevient instantanément le comportement historique.

## 9. Exposition front (jamais partenaires)

`TenantModel` (profil locataire uniquement, `@AfterMapping` court-circuité si `userApi != null`) :
- `lotteryStatus` : `PENDING` | `DRAWN` | `COOLDOWN` — **absent** si flag OFF ou sans état en cours (le front ne connaît jamais le flag) ;
- `nextEligibleDate` : uniquement avec `COOLDOWN`.

## 10. Abandon de l'ETA locataire

Le calcul « traité entre le … et le … » (`GET /api/tenant/{id}/expectedProcessingTime`) est abandonné côté front : `dailyCount` ne peut pas être à la fois le débit total de l'ETA et l'assiette des places du tirage. L'endpoint, `ProcessingCapacityService(Impl)` et `getTenantRank` seront supprimés dans un **commit séparé déployé après le front**. `ProcessingCapacityRepository.findByDate` et `countProcessedDossiersFromToday` restent (écran BO, job).

## 11. Mails

| Situation | Template | Propriété |
|---|---|---|
| Candidature enregistrée | aucun mail (confirmation à l'écran) | — |
| Tiré au sort (ou flush) → entrée en file | **aucun mail** — rien à faire, le verdict opérateur (validé / refusé) notifie | — |
| Non tiré | **aucun mail** (quota Brevo) — état visible sur le tableau de bord | — |
| Fin de cooldown (J+3) | **nouveau template à créer** | `brevo.template.id.lottery.cooldown.ended` |

## 12. Observabilité

- `tenant_log` : `QUEUE_ENTERED` (+ `source` / `bypass`), `LOTTERY_DRAWN`, `LOTTERY_NOT_DRAWN`, `LOTTERY_TICKET_CANCELLED`.
- `lottery_draw` : une ligne par tirage (capacité, bypass, places, tickets, tirés).
- ELK : tâche `TENANT_LOTTERY_DRAW` ; surveiller « no processing capacity defined » et « no available slot ».
- Métriques SQL : taux de candidature par tirage (`ticket_count` vs `VALIDATION_REQUESTED`), taux de recandidature après `LOTTERY_NOT_DRAWN`, part de `CANCELLED`.

**Dette planifiée** — le jour où tous les dossiers passent par le tirage (tous types d'application, partenaires intégrés, rollout 100 %), le bypass disparaît (seul `BO_REPROCESS` subsiste, négligeable) : `places = capacité`, plus de comptage ni de colonne `bypass_count`. Tout ce qui est à retirer est marqué `TODO(lottery-bypass)` dans le code.

## 13. Séquence de déploiement

1. Déployer BO/api-tenant (migration Liquibase), **puis** task-scheduler (`spring.liquibase.enabled=false` chez lui). Flag OFF : comportement inchangé ; tickets de reprise créés ; `QUEUE_ENTERED` commence à s'accumuler.
2. Créer le template Brevo « fin de cooldown » et renseigner `brevo.template.id.lottery.cooldown.ended`.
3. Déployer le frontend (nouveaux états de l'encart, wording neutre compatible flag OFF).
4. Saisir les capacités sur `/bo/admin/process/capacities`. En préprod seulement : `lottery.draw.allow-multiple-per-day=true`.
5. Activer `tenant_lottery` sur `/bo/feature-flags` — **au moins 24 h après l'étape 1** (bypass mesuré sur J−1 ; trop tôt, X serait surestimé). Surveiller le premier tirage.
6. Commit de nettoyage ETA backend (§10).
7. **Rollback** : désactiver le flag (flush automatique).

## 14. Scénarios de test manuel (préprod, flag ON, rollout opt-in 100 %)

| # | Scénario | Attendu |
|---|---|---|
| L1 | Dossier COMPLETED → clic « demander une vérification » | Dossier reste `COMPLETED`, ticket `PENDING`, `validation_requested=true`, aucun mail, front « candidature enregistrée » |
| L2 | Saisir la capacité du jour puis bouton BO « Lancer le tirage du jour » | Ligne `lottery_draw` cohérente ; tirés : `DRAWN`, `TO_PROCESS`, aucun mail, présents dans `ranked_tenant` ; non-tirés : `NOT_DRAWN`, `cooldown_until=J+3`, aucun mail |
| L3 | Retenter le tirage après exécution | Bouton masqué, détail du tirage affiché ; un POST direct répond « déjà eu lieu à HHhMM » sans rien rejouer |
| L4 | Non-tiré → clic opt-in avant J+3 | `409`, bouton front désactivé avec date de recandidature |
| L5 | Non-tiré → passage à J+3 (job de la nuit) | Mail « vous pouvez recandidater », `cooldown_notified_at` posé, clic → nouveau `PENDING` |
| L6 | Tiré non traité → annulation (clic « non ») | Ticket `CANCELLED`, retour `COMPLETED`, sort de la file |
| L7 | Tiré non traité → modification document → re-signature | Retour `TO_PROCESS` (ticket `DRAWN`), `QUEUE_ENTERED` avec `bypass = false` |
| L8 | Tiré → validé (ou refusé) → modification → re-signature | Ticket `CONSUMED` ; re-soumission en `COMPLETED`, encart de candidature réaffiché |
| L9 | Candidat `PENDING` → liaison partenaire DFC | Bascule `TO_PROCESS`, ticket `CANCELLED`, `QUEUE_ENTERED` `PARTNER_LINK` avec `bypass = true` |
| L10 | Candidat `PENDING` → passage en COUPLE | `validation_requested` purgé, ticket `CANCELLED` |
| L11 | Capacité non saisie à minuit | Log.warn, pas de tirage enregistré, tickets intacts, bouton BO de relance visible |
| L12 | Capacité 5, bypass mesuré 8 (X = −3) | `lottery_draw` avec `available_slots = −3`, `drawn_count = 0` ; tickets toujours `PENDING`, pas de `cooldown_until` ; le tirage du lendemain les inclut |
| L13 | Candidat `PENDING` → dossier `INCOMPLETE` au moment du tirage | Ticket `CANCELLED` (pas de cooldown) ; après re-signature l'encart réapparaît, nouveau clic → nouveau ticket |
| L14 | Désactivation du flag avec des `PENDING` | Tickets flushés `DRAWN` + `TO_PROCESS` (aucun mail) ; clic opt-in → `TO_PROCESS` immédiat |
| L15 | (préprod) `lottery.draw.allow-multiple-per-day=true` | Le bouton BO reste disponible ; chaque clic crée un nouveau `lottery_draw` pour la même date |
| L16 | Vérif partenaires | Aucun champ lottery dans les payloads DFC/api-partner/api-owner ; `callback_log.tenant_status` jamais `COMPLETED` |
