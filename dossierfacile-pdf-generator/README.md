# PDF File Generator (dossierfacile-pdf-generator)

## Description
Service dedicated to the generation of PDF documents.

## Main Features
- Conversion of documents and files to PDF with watermark
- Generation of rental files in PDF format

## Configuration :

Create a file `application-dev.properties` in `dossierfacile-pdf-generator/src/main/resources`
```properties
# Storage path for the MockStorage
mock.storage.path=../mock-storage
# Storage provider list (Options: LOCAL, OVH, OUTSCALE, S3)
# S3 is the new multi-AZ provider with bucket-based storage
storage.provider.list=LOCAL

# S3 Configuration (for new multi-AZ provider)
# Required if storage.provider.list includes S3
s3.region=sbg
s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
s3.access.key=
s3.secret.access.key=
# S3 Bucket names (optional, defaults shown)
s3.bucket.watermark.doc.name=watermark-doc
s3.bucket.full.pdf.name=full-pdf

#URL of the database
spring.datasource.url=
#Username of the database
spring.datasource.username=
#Password of the database
spring.datasource.password=

#Keycloak configuration
#Keycloak Url
keycloak.server.url=http://localhost:8085/auth
#Keycloak Realm
keycloak.server.realm=dossier-facile
#Keycloak Client Id for admin purpose => need to be inside the realm Master 
keycloak.server.client.id=dossier-facile-api
#Keycloak secret that need to be retrieved from keycloak. 
keycloak.server.client.secret=

#Pdf generator configuration
#key used to generate a certificate
pdf.certificate=
#Is the signature of the pdf activated (default is false)
pdf.signature.activation=
# Key used to sign the pdf
pdf.private_key=

#RabbitMQ
# Number of message a worker will receive at a time before he acknowledge them
rabbitmq.prefetch=
# Name of the queue apartment-sharing
rabbitmq.queue.apartment-sharing.name=
# Name of the queue watermark-generic
rabbitmq.queue.watermark-generic.name=
# Routing key for the apartment-sharing queue
rabbitmq.routing.key.apartment-sharing=
# Routing key for the watermark-generic queue
rabbitmq.routing.key.watermark-generic=
#Configuration for rabbitmq (example with the docker-compose)
spring.rabbitmq.username=dev
spring.rabbitmq.password=password
spring.rabbitmq.host=127.0.0.1

#Brevo config
# Leave this empty for dev
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
