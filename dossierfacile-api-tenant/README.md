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

Create a file `application-dev.properties` in `dossierfacile-api-tenant/src/main/resources`

```properties
#Path to the folder when the files will be saved during dev
mock.storage.path=../mock-storage
# List of the providers to use for the storage
storage.provider.list=LOCAL
# Api version
api.version=3
# Url of the tenant front
tenant.base.url=localhost:9002
# Port of the API
server.port=8090
# Url of the tenant API
application.base.url=http://localhost:8090

# TODO: replace with your database credentials if changed in root docker-compose-dev.yml
# URL of the database
spring.datasource.url=jdbc:postgresql://localhost:5432/dossierfacile
# Username of the database
spring.datasource.username=dossierfacile
# Password of the database
spring.datasource.password=your_very_secure_password

#Configuration for rabbitmq (example with the docker-compose)
spring.rabbitmq.username=dev
spring.rabbitmq.password=password
spring.rabbitmq.host=127.0.0.1

#Keycloak configuration
#Keycloak Url
keycloak.server.url=http://localhost:8085/auth
#Keycloak Realm
keycloak.server.realm=dossier-facile
#Keycloak Client Id for admin purpose => need to be inside the realm Master
keycloak.server.client.id=dossier-facile-api
#Keycloak secret that need to be retrieved from `keycloak > master realm > Clients > dossier-facile-api > Credentials > Client Secret`
keycloak.server.client.secret=<REPLACE_ME_KEYCLOAK_SECRET>
#Issuer URI of the keycloak server
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8085/auth/realms/dossier-facile
#JWK set URI of the keycloak server
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8085/auth/realms/dossier-facile/protocol/openid-connect/certs

# Log configuration
environment=localhost
logging.config=classpath:logback-spring-delayed.xml
logging.level.root=INFO
logging.path=logs

#Brevo config
# Leave this empty for dev
brevo.apikey=
brevo.template.id.token.expiration=
brevo.template.id.message.notification=
brevo.template.id.account.deleted=
brevo.template.id.account.satisf=
brevo.template.id.account.email.validation.reminder=
brevo.template.id.account.incomplete.reminder=
brevo.template.id.account.completed=
brevo.template.id.account.completed.with.partner=
brevo.template.id.account.declined.reminder=
brevo.template.id.dossier.validated=
brevo.template.id.welcome=
brevo.template.id.new.password=
brevo.template.id.contact.support=
brevo.template.id.invitation.couple=
brevo.template.id.invitation.group=
brevo.template.id.welcome.partner=
brevo.template.id.share.file=
brevo.template.id.application.couple.invitation.existing=
brevo.template.id.application.group.invitation.existing=
brevo.template.id.partner.access.revoked=
# Use to blacklist domains default value is : example.com
brevo.domains.blacklist=
brevo.template.id.message.notification=
brevo.template.id.message.notification.with.details=
brevo.template.id.account.deleted=
brevo.template.id.dossier.fully.validated=
brevo.template.id.dossier.tenant.denied=
brevo.template.id.message.notification.with.partner=
brevo.template.id.message.notification.with.partner.and.details=
brevo.template.id.dossier.fully.validated.with.partner=
brevo.template.id.dossier.tenant.denied.with.partner=
brevo.template.id.dossier.tenant.denied.with.partner.and.details=
brevo.template.id.dossier.tenant.denied.with.details=
brevo.template.id.tenant.validated.dossier.validated=
brevo.template.id.tenant.validated.dossier.validated.w.partner=
brevo.template.id.tenant.validated.dossier.not.valid=
brevo.template.id.tenant.validated.dossier.not.valid.w.partner=
link.after.denied.default=
link.after.validated.default=
link.shared.property=

# Document IA
document.ia.api.base.url=
document.ia.api.key=
```

## LogStash :

For the dev environment the appender Logstash is disabled by default.

## Database :

⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

## Run the application

```shell
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```
