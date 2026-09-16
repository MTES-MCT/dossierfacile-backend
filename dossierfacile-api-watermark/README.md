# API Watermark (dossierfacile-api-watermark)

## Description
REST API dedicated to the FiligraneFacile project.

## Main Features
- Add files that will be processed
- download the watermarked files

## Configuration
Copy [`src/main/resources/application-dev.properties.example`](src/main/resources/application-dev.properties.example) to `src/main/resources/application-dev.properties` (git-ignored, loaded by the `dev` profile) and fill in the `<REPLACE_ME>` values. The example file is the reference for every property this module reads: keep it up to date when adding one.

# LogStash :

For the dev environment the appender Logstash is disabled by default.

# Database :
⚠️ The database is managed by the project dossierfacile-api-tenant.

# Run the application

```shell
    mvn spring-boot:run -D  spring-boot.run.profiles=dev,mockOvh
```