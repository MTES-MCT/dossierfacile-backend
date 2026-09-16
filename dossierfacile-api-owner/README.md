# API Propriétaire (dossierfacile-api-owner)

## Description

Rest API dedicated to the owner space, allowing the management and consultation of tenant files.

## Functionalities

- Creation and management of owner accounts
- Creation and management of owner properties
- Creation and management of owner leases

## Configuration:

Copy [`src/main/resources/application-dev.properties.example`](src/main/resources/application-dev.properties.example) to `src/main/resources/application-dev.properties` (git-ignored, loaded by the `dev` profile) and fill in the `<REPLACE_ME>` values. The example file is the reference for every property this module reads: keep it up to date when adding one.

## LogStash

For the dev environment the appender Logstash is disabled by default.

## Database

⚠️ The database is managed by the project dossierfacile-api-tenant.

## Run the application

```shell
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```

## Important information

- There is a specific configuration for the routes /webhook/\* with a basic auth configured inside the properties.
- A cron task is running every day at 2am to send follow-up emails to the owners.
