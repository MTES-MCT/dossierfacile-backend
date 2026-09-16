# PDF File Generator (dossierfacile-pdf-generator)

## Description

Service dedicated to the generation of PDF documents.

## Main Features

- Conversion of documents and files to PDF with watermark
- Generation of rental files in PDF format

## Configuration :

Copy [`src/main/resources/application-dev.properties.example`](src/main/resources/application-dev.properties.example) to `src/main/resources/application-dev.properties` (git-ignored, loaded by the `dev` profile) and fill in the `<REPLACE_ME>` values. The example file is the reference for every property this module reads: keep it up to date when adding one.

## LogStash

For the dev environment the appender Logstash is disabled by default.

## Database

⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

## Run the application

```shell
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```
