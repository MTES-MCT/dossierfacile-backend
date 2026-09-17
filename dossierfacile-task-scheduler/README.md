# Asynchronous Task Management Service (dossierfacile-task-scheduler)

## Description

The **dossierfacile-task-scheduler** service is a key component of our system, dedicated to executing scheduled and
recurring tasks. It is based on the **Spring Boot** framework and uses the `@Scheduled` annotation to define and manage
the execution frequency.

Tasks are configured in two ways:

- **Either periodically**: using a fixed duration between each execution (`fixedRate` or `fixedDelay`).
- **Or based on a Cron expression**: allowing specific moments in time to be defined (e.g., "every day at 3 AM" or "the
  first day of each month at midnight").

This service allows periodic asynchronous operations that could negatively impact the performance of applications
requiring high availability if they were performed on those services.

Furthermore, it also allows critical services to be scaled horizontally, thus ensuring efficient management of
increasing loads and activity peaks.

## Tasks:


| Cron Name                                                          | Cron Description                                                                                                                                                                                                                                                                                                                                                         | Execution Frequency                              | Entry point                                            |
|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|--------------------------------------------------------|
| `scheduled.process.check.api.ademe`                                | Check the availability of the ADEME API. Property : `scheduled.process.check.api.ademe`control the execution delay in minutes.                                                                                                                                                                                                                                           | Every 10 minutes                                 | `CheckAdemeApiTask.checkAdemeApi`                      |
| `cron.process.pdf.generation.failed`                               | This task will check the documents where the PDF failed to be generated and try again (200 documents at a time)                                                                                                                                                                                                                                                          | Every day at 1:30 AM, 7:30 AM, 12:30 PM, 7:30 PM | `DocumentTask.reLaunchFailedPDFGeneration`             |
| `cron.delete.document.with.failed.pdf`                             | Delete the documents where PDF are broken, Scan the error documents and remove after some time (`document.pdf.failed.delay.before.delete.hours`). After a deletion it will send an email to the tenant                                                                                                                                                                   | Every day at 6:00 AM and 10:00 PM                | `DocumentTask.deleteDocumentWithFailedPdfGeneration`   |
| `garbage-collection`                                               | Deletion of orphaned files, files without link inside the database. Uses an optimized incremental strategy with the `garbage_sequence` table. The process is managed by configuration that is enabled only when `garbage-collection.enabled=true`. It's possible to control the number of files by iteration : `garbage-collection.objects-by-iteration` and the delay of execution `garbage-collection.seconds-between-iterations`                | Every minute                                     | `GarbageCollectionTask.cleanGarbage`                   |
| `cron.owner.delete`                                                | If the first period of activity is reached `owner_weeks_for_first_warning_deletion` send a first warning to the owner by email. After `owner_weeks_for_second_warning_deletion` send a second warning. After `owner_weeks_for_deletion` delete the account and send an email.                                                                                            | Every Friday at 10:10 AM                         | `OwnerWarningTask.accountWarningsForDocumentDeletion`  |
| `scheduled.process.storage.delete.delay.ms`                        | Delete the files flag as "TO_DELETE" from the api. If the delete failed, the flag "DELETE_FAILED" is added to perform a retry. property `scheduled.process.storage.delete.delay.ms` control the execution delay in MS                                                                                                                                                    | Every 10 secondes                                | `DeleteFilesTask.deleteFileInProviderTask`             |
| `scheduled.process.storage.delete.retry.dailed.delay.minutes`      | Delete the files flag as "DELETE_FAILED" from the delete task. Property `scheduled.process.storage.delete.retry.dailed.delay.minutes` control the execution delay in minutes                                                                                                                                                                                             | Every 5 minutes                                  | `DeleteFilesTask.retryDeleteFileInProviderTask`        |
| ~~`scheduled.process.storage.backup.delay.ms`~~                    | **Removed** - No longer needed with S3 multi-AZ provider | -                                 | -                   |
| ~~`scheduled.process.storage.backup.retry.failed.copy.delay.minutes`~~ | **Removed** - No longer needed with S3 multi-AZ provider | -                              | -                  |
| `cron.process.warnings`                                            | Delete the documents of a tenant if the account is inactive. First warning is sent after `days_for_first_warning_deletion` days (default: 30), second warning after `days_for_second_warning_deletion` days (default: 37), and documents are deleted after `days_for_deletion_of_documents` days (default: 45). The account is then archived. | Every monday at 10:20 AM                             | `TenantWarningTask.accountWarningsForDocumentDeletion` |
| `cron.account-deletion`                                            | Delete the tenant account after a period `months_for_deletion_of_archived_tenants` month of inactivity when the account is archived                                                                                                                                                                                                                                      | Every Monday at 7:10 AM                          | `TenantDeletionTask.deleteOldAccounts`                 |
| `scheduled.process.documentia.synchronization.delay.secondes`      | Synchronise the document ia analyse that has not been catch by the api tenant webhook. property `scheduled.process.documentia.synchronization.delay.secondes` control the execution delay between 2 executions                                                                                                                                                                                                                      | Every 10 Secondes                                | `SynchroniseDocumentIATask.synchroniseDocumentIA`      |
| `lottery.draw.cron`                                                | Daily verification lottery (see `docs/tenant-lottery.md`): draws `capacity − bypass` pending applications into the operator queue, puts the others in cooldown (`lottery.cooldown.days`, default 3), then mails the tenants whose cooldown ended. No-op when the `tenant_lottery` feature flag is off. Can also be launched from the BO capacities screen.                                                                     | Every day at 00:05 (Europe/Paris)                | `LotteryDrawTask.dailyLotteryDraw`                     |

## Configuration

Copy [`src/main/resources/application-dev.properties.example`](src/main/resources/application-dev.properties.example) to `src/main/resources/application-dev.properties` (git-ignored, loaded by the `dev` profile) and fill in the `<REPLACE_ME>` values. The example file is the reference for every property this module reads: keep it up to date when adding one.

## LogStash :

For the dev environment the appender Logstash is disabled by default.

## Database :

⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

## Run the application

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=dev,mockOvh
```

---

## One-Off Task: Réplication & Anonymisation Analytics

Cette tâche remplace le DAG Airflow historique. Elle réplique et anonymise les données depuis la base PostgreSQL Scalingo (production) vers la base PostgreSQL OVH (dbt / Metabase).

### Principes clés
- **Isolation totale** : Portée par l'application dédiée [`AnalyticsReplicationApplication`](src/main/java/fr/dossierfacile/scheduler/AnalyticsReplicationApplication.java). Elle ne démarre aucun serveur web (Tomcat), aucune tâche `@Scheduled` de l'application principale, et aucune couche JPA/Hibernate.
- **Streaming natif JDBC (Zero Heap OOM)** : Transfert par buffer mémoire de 64 Ko avec `CopyManager` PostgreSQL (`copyOut` vers `copyIn`) via un thread producteur dédié et `PipedInputStream` / `PipedOutputStream`.
- **Garde-fous anti-destruction de la production** :
  - **Verrouillage lecture seule de la source** : La connexion vers la base source est verrouillée via `sourceConn.setReadOnly(true)` dès son ouverture. PostgreSQL interdit ainsi physiquement tout ordre DDL (`DROP`, `ALTER`, `TRUNCATE`) ou DML (`INSERT`, `UPDATE`, `DELETE`) sur la base de production.
  - **Kill-Switch d'identité physique** : Le job vérifie la non-égalité des URLs source et destination et interroge PostgreSQL (`inet_server_addr()`, `inet_server_port()`, `current_database()`). Si la source et la destination résolvent vers la même base physique, le job s'interrompt immédiatement avec une erreur fatale avant toute opération.
- **Zero-Downtime Swap** : Copie dans des tables temporaires (`tmp_<table>`), suivie d'une bascule atomique globale dans une transaction unique (`DROP TABLE IF EXISTS <table> CASCADE; ALTER TABLE tmp_<table> RENAME TO <table>;`).
- **Conformité RGPD stricte** : Whitelist de 25 tables ([`AnalyticsTableMapping`](src/main/java/fr/dossierfacile/scheduler/tasks/analytics/AnalyticsTableMapping.java)). Exclusion de toutes les données personnelles (noms, emails, adresses, etc.) et hachage salé SHA-256 des tokens (`encode(sha256((token || '<salt>')::bytea), 'hex')`).
- **Enchaînement dbt optionnel** : Déclenche le webhook POST vers dbt si configuré (sinon log `Pas de trigger DBT, skip.`).
- **Code de sortie système** : `0` en cas de succès, `1` en cas d'erreur (remontée dans Scalingo et ELK via `TaskName.REPLICATE_ANALYTICS`).

### Génération du SALT secret

Le sel (`ANALYTICS_SALT`) doit être une chaîne secrète robuste et non vide. Vous pouvez le générer avec l'une des commandes suivantes :

```shell
# Recommandé (32 octets aléatoires encodés en hexadécimal, 64 caractères) :
openssl rand -hex 32

# Alternative en Base64 :
openssl rand -base64 32
```

### Variables d'environnement

| Variable | Description | Obligatoire |
|---|---|---|
| `ANALYTICS_SALT` | Sel secret utilisé pour hacher les tokens RGPD | **Oui** |
| `ANALYTICS_DEST_DB_URL` | URL JDBC de la base destination OVH (`jdbc:postgresql://...`) | **Oui** |
| `ANALYTICS_DEST_DB_USER` | Utilisateur de la base destination | **Oui** |
| `ANALYTICS_DEST_DB_PASSWORD` | Mot de passe de la base destination | **Oui** |
| `DBT_WEBHOOK_URL` | URL du webhook dbt à déclencher après la bascule | Non (optionnel) |
| `DBT_WEBHOOK_TOKEN` | Bearer token d'authentification pour le webhook dbt | Non (optionnel) |

> Note : La base source utilise par défaut `spring.datasource.url`, `spring.datasource.username` et `spring.datasource.password`.

### Exécution locale avec Maven

```shell
ANALYTICS_SALT="$(openssl rand -hex 32)" \
ANALYTICS_DEST_DB_URL="jdbc:postgresql://localhost:5432/analytics_dest" \
ANALYTICS_DEST_DB_USER="mon_user" \
ANALYTICS_DEST_DB_PASSWORD="mon_password" \
mvn spring-boot:run -pl dossierfacile-task-scheduler \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.main-class=fr.dossierfacile.scheduler.AnalyticsReplicationApplication \
  -Dspring-boot.run.arguments="--run-task=replicate-analytics"
```

### Exécution sur Scalingo (Tâche planifiée)

> **Pourquoi pas de `cron.json` dans le dépôt Git ?**  
> Ce dépôt multi-modules déploie plusieurs applications Scalingo distinctes (`api-tenant`, `bo`, `api-owner`, `task-scheduler`...). Si un fichier `cron.json` était placé à la racine du dépôt, Scalingo l'enregistrerait automatiquement sur **l'ensemble des applications**, déclenchant des conteneurs inutiles sur les autres services.  
> La tâche est donc configurée **uniquement sur l'application Scalingo `task-scheduler`**, sans fichier `cron.json`.

#### Configuration sur l'app Scalingo `task-scheduler`

**Option A : Via le Dashboard Scalingo**
1. Accéder à l'application **`task-scheduler`** sur Scalingo.
2. Aller dans l'onglet **Scheduler** (ou Tâches planifiées).
3. Ajouter une nouvelle tâche :
   - **Taille du conteneur** : `L`
   - **Fréquence (cron)** : `0 3 * * *` (ou l'heure souhaitée, ex. 03h00 UTC)
   - **Commande** :
     ```bash
     java $JVM_OPTIONS -Djna.library.path=$JNA_LIBRARY_PATH -cp $APP_DIR/target/$APP_DIR.jar -Dloader.main=fr.dossierfacile.scheduler.AnalyticsReplicationApplication org.springframework.boot.loader.launch.PropertiesLauncher
     ```

**Option B : Via la CLI Scalingo**
```shell
scalingo --app <nom-app-task-scheduler> cron-jobs:add \
  --cron "0 3 * * *" \
  --size L \
  --command "java \$JVM_OPTIONS -Djna.library.path=\$JNA_LIBRARY_PATH -cp \$APP_DIR/target/\$APP_DIR.jar -Dloader.main=fr.dossierfacile.scheduler.AnalyticsReplicationApplication org.springframework.boot.loader.launch.PropertiesLauncher"
```

#### Test manuel ponctuel (One-off container)
Pour déclencher manuellement la réplication sur Scalingo sans attendre le cron :
```shell
scalingo --app <nom-app-task-scheduler> run --size L \
  "java \$JVM_OPTIONS -Djna.library.path=\$JNA_LIBRARY_PATH -cp \$APP_DIR/target/\$APP_DIR.jar -Dloader.main=fr.dossierfacile.scheduler.AnalyticsReplicationApplication org.springframework.boot.loader.launch.PropertiesLauncher"
```