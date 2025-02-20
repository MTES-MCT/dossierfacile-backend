# API Propriétaire (dossierfacile-api-owner)

## Description
Rest API dedicated to the owner space, allowing the management and consultation of tenant files.

## Functionalities

- Creation and management of owner accounts
- Creation and management of owner properties
- Creation and management of owner leases

## Configuration : 

Create a file `application-dev.properties` in `dossierfacile-api-owner/src/main/resources`

```properties
#Path to the folder when the files will be saved during dev
mock.storage.path=/path/to/mock-storage/
#Url of the owner front
owner.url=http://localhost:3000
#Port of this API
server.port=8083

#URL of the database
spring.datasource.url=
#Username of the database
spring.datasource.username=
#Password of the database
spring.datasource.password=

#Variable used to define the number of days after the creation of a property that the owner will receive a follow-up email
days.after.validated.property.to.follow.up.email=42

# Token to be used as basic auth for the webhook routes /webhook/*
callback.http.auth.token=
# name of the header used to pass the token
callback.http.auth.token.header.name=

#Keycloak configuration
#Keycloak Url
keycloak.server.url=http://localhost:8085/auth
#Keycloak Realm
keycloak.server.realm=dossier-facile
#Keycloak Client Id for admin purpose => need to be inside the realm Master 
keycloak.server.client.id=dossier-facile-api
#Keycloak secret that need to be retrieved from keycloak. 
keycloak.server.client.secret=
#Issuer URI of the keycloak server
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8085/auth/realms/dossier-facile-owner
#JWK set URI of the keycloak server
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8085/auth/realms/dossier-facile-owner/protocol/openid-connect/certs
#Issuer URI of the tenant keycloak server
tenant.jwt.issuer-uri=http://localhost:8085/auth/realms/dossier-facile
#JWK set URI of the tenant keycloak server
tenant.jwt.jwk-set-uri=http://localhost:8085/auth/realms/dossier-facile/protocol/openid-connect/certs

#Brevo config
# Leave this empty for dev
brevo.apikey=
brevo.template.id.welcome=
brevo.template.id.new.password=
brevo.template.id.applicant.validated=
brevo.template.id.new.applicant=
brevo.template.id.new.applicant.validated=
brevo.template.id.new.applicant.not.validated=
brevo.template.id.validated.property=
brevo.template.id.follow-up.validated.property=
```
# LogStash :

For the dev environment the appender Logstash is disabled by default.

# Database :
⚠️ The database is managed by the project dossierfacile-api-tenant.

# Run the application

```shell
    ./mvnw -pl dossierfacile-api-owner spring-boot:run -Dspring-boot.run.profiles=dev,mockOvh
```

# Important information : 
 - There is a specific configuration for the routes /webhook/* with a basic auth configured inside the properties.
 - A cron task is running every day at 2am to send follow-up emails to the owners.


