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

## Configuration

Create a file `application-dev.properties` in `dossierfacile-task-scheduler/src/main/resources`

```properties
spring.config.import=file:../.env[.properties]
mock.storage.path=../mock-storage
storage.provider.list=LOCAL
# For S3 provider (OVH Multi-AZ): storage.provider.list=S3
# s3.region=sbg
# s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
# s3.access.key=
# s3.secret.access.key=
server.port=8089
spring.datasource.url=jdbc:postgresql://localhost:5432/dossierfacile
spring.datasource.username=dossierfacile
spring.datasource.password=your_very_secure_password
# keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=dossier-facile-api
keycloak.server.client.secret=
# email service
tenant.base.url=localhost:9002
owner.base.url=localhost:3000
brevo.apikey=
brevo.template.id.first.warning.for.deletion.of.documents=
brevo.template.id.second.warning.for.deletion.of.documents=
brevo.template.id.deleted.document.with.failed.pdf=
#Cron :
#Cron to delete accounts (Tous les lundi a 7h10)
cron.account-deletion=0 10 7 * * 1
#number of month for archived tenants to be deleted (default: 12)
months_for_deletion_of_archived_tenants=
#Cron to delete old owner accounts (default: 10 10 * * * 5) Run every friday at 10:10 am
cron.owner.delete=
# Number of weeks for deletion of owner accounts (default: 104)
owner_weeks_for_deletion=
# Number of weeks for first warning deletion of owner accounts (default: 100)
owner_weeks_for_first_warning_deletion=
# Number of weeks for second warning deletion of owner accounts (default: 103)
owner_weeks_for_second_warning_deletion=
# Cron to trigger the generation of the pdf when a document is in error (Run every day, at 1:30 am, 7:30 am, 12:30 pm, 7:30 pm)
cron.process.pdf.generation.failed=0 30 1,7,12,19 * * *
# Cron to warn deletion of tenant account (Run every monday at 10:20 am)
cron.process.warnings=0 20 10 * * 1
# Number of days for first warning deletion of documents (default: 30)
days_for_first_warning_deletion=30
# Number of days for second warning deletion of documents (default: 37)
days_for_second_warning_deletion=37
# Number of days for deletion of documents (default: 45)
days_for_deletion_of_documents=45
# Cron to delete documents with failed pdf (Run every day at 6:00 am and 10:00 pm)
cron.delete.document.with.failed.pdf=0 0 6,22 * * *
# Number of hours to delete documents with failed pdf
document.pdf.failed.delay.before.delete.hours=48
#Delay between to execution of check Ademe API (default: 10) Every 10 minutes
scheduled.process.check.api.ademe=
#Delete files flag by TO_DELETE in database (every 10 seconds)
scheduled.process.storage.delete.delay.ms=10000
#Retry to delete files flag to "DELETE_FAILED" in database (every 5 minutes)
scheduled.process.storage.delete.retry.failed.delay.minutes=5
#Copy files to the other storage (every 10 seconds)
scheduled.process.storage.backup.delay.ms=10000
# Retry to copy files flag with COPY_FAILED to the other storage (every 5 minutes)
scheduled.process.storage.backup.retry.failed.copy.delay.minutes=5
# If this value is set to true, register the bean GarbageCollectionConfiguration. Activate the deletion of orphelin files
garbage-collection.enabled=
# Number of files to handle at every iteration (default: 100)
garbage-collection.objects-by-iteration=
# Number of seconds between two iterations
garbage-collection.seconds-between-iterations=60
environment=local
application.api.version=4

#Ademe api configuration 
ademe.api.base.url=https://prd-x-ademe-externe-api.de-c1.eu1.cloudhub.io/api/v1
ademe.api.client.id=
ademe.api.client.secret=
```

## LogStash :

For the dev environment the appender Logstash is disabled by default.

## Database :

⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

## Run the application

```shell
mvn spring-boot:run -D  spring-boot.run.profiles=dev,mockOvh
```