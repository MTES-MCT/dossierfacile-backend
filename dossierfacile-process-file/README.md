# Asynchronous File and Document Processing Service (dossierfacile-process-file)

## Description
Service responsible for the asynchronous processing of documents and files.

## Main Features
- File miniaturization
- Data extraction (OCR, 2D Doc reading, ...)
- Document analysis

## Configuration
Create a file `application-dev.properties` in `dossierfacile-process-file/src/main/resources`

```properties
# Storage path for the MockStorage
mock.storage.path=../mock-storage
# Storage provider list
storage.provider.list=LOCAL
# Port of this service
server.port=8088

#URL of the database
spring.datasource.url=
#Username of the database
spring.datasource.username=
#Password of the database
spring.datasource.password=

# Time before the document will be picked base on the createAt timestamp.
document.analysis.delay.ms=10000
# Timeout for the document analysis.
document.analysis.timeout.ms=90000

#Url to payfit api
payfit.api.url=

#Url to verification document api of france identite (default :https://dossierfacile-france-identite-numerique-api.osc-secnum-fr1.scalingo.io/api/validation/v1/check-doc-valid?all-attributes=true)
france.identite.api.url=

# Brevo
brevo.apikey=
```

# LogStash :

For the dev environment the appender Logstash is disabled by default.

# Database :
⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

# Run the application

```shell
mvn spring-boot:run -D mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```



