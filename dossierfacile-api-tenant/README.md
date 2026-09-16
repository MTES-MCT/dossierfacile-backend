# API Tenant (dossierfacile-api-tenant)

## Description

REST API dedicated to tenants, allowing the management of rental applications.  
REST API dedicated to DossierFacile (DFC) users, allowing the visualization of tenant information.

## Main Features

- Creation and management of tenant accounts
- Upload and management of supporting documents
- Creation and modification of applications
- Sharing of applications

## DFC API Documentation

The Swagger documentation is available [here](https://api-preprod.dossierfacile.fr/swagger-ui/index.html?urls.primaryName=API%20DFC).

## Configuration

Copy [`src/main/resources/application-dev.properties.example`](src/main/resources/application-dev.properties.example) to `src/main/resources/application-dev.properties` (git-ignored, loaded by the `dev` profile) and fill in the `<REPLACE_ME>` values. The example file is the reference for every property this module reads: keep it up to date when adding one.

## LogStash :

For the dev environment the appender Logstash is disabled by default.

## Database :

⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

## Run the application

```shell
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```
