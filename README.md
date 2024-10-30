# DossierFacile Back-end

<pre>
DossierFacile.fr a été créé par le Ministère de la Transition écologique
pour aider à la réalisation de dossiers de location.
</pre>

The project is available at [DossierFacile.fr](https://dossierfacile.fr).

The front-end code is also accessible in [this repository](https://github.com/MTES-MCT/Dossier-Facile-Frontend).

## Prerequisites

You need to have [Java JDK 21](https://openjdk.org/projects/jdk/21/), [maven](https://maven.apache.org/) and [PostgreSQL](https://www.postgresql.org/) installed.

### Database

Create a dedicated user and database for dossierfacile.

If you want to use [Keycloak](https://www.keycloak.org/) locally, you need to create another database for it.

### Keycloak

You need [Docker](https://www.docker.com/) and [Docker compose](https://docs.docker.com/compose/).

Clone the [DossierFacile-Keycloak project](https://github.com/MTES-MCT/Dossier-Facile-Keycloak).

Download [keycloak-franceconnect-v7.0.0.jar](https://github.com/InseeFr/Keycloak-FranceConnect/releases/download/7.0.0/keycloak-franceconnect-7.0.0.jar) and copy it inside `Dossier-Facile-Keycloak`.

Use the following Dockerfile:

```dockerfile
FROM quay.io/keycloak/keycloak:22.0
ENV TZ=Europe/Paris
COPY keycloak-franceconnect-7.0.0.jar /opt/keycloak/standalone/deployments/keycloak-franceconnect-7.0.0.jar
ADD theme/df /opt/keycloak/themes/df
ADD theme/df-owner /opt/keycloak/themes/df-owner

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]
```

Create `docker-compose.yml` and update it with your database settings.

```YAML
version: '2'
services:
    keycloak:
        build: .
        environment:
            - KEYCLOAK_CREATE_ADMIN_USER=false
            - KEYCLOAK_ADMIN_USER=admin
            - KEYCLOAK_ADMIN_PASSWORD=admin
            - KC_DB=postgres
            - KC_DB_URL=jdbc:postgresql://localhost:5433/keycloak
            - KC_DB_PASSWORD=
            - KC_DB_USERNAME=
            - KC_HTTP_RELATIVE_PATH=/auth
        ports:
            - "8085:8080"
```

Use `docker compose up` to launch.

### Config

Create a new folder `mock_storage` to store files.

Create the file `application-dev.properties` in `dossierfacile-bo/src/main/resources`

```properties
mock.storage.path=/path/to/mock_storage/
storage.provider.list=LOCAL
server.port=8081
application.domain=http://localhost:8081
domain.protocol=http
# SQL
spring.datasource.url=jdbc:postgresql://localhost:5433/dossierfacile
spring.datasource.username=
spring.datasource.password=
# Keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=dossier-facile-api
keycloak.server.client.secret=
# Brevo
brevo.apikey=
```

Create the file `application-dev.properties` in `dossierfacile-api-tenant/src/main/resources`

```properties
mock.storage.path=/path/to/mock_storage/
storage.provider.list=LOCAL
api.version=3
tenant.base.url=localhost:9002
server.port=8090
application.base.url=http://localhost:8090
# SQL
spring.datasource.url=jdbc:postgresql://localhost:5433/dossierfacile
spring.datasource.username=
spring.datasource.password=
# Keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=dossier-facile-api
keycloak.server.client.secret=
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8085/auth/realms/dossier-facile
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8085/auth/realms/dossier-facile/protocol/openid-connect/certs
# Brevo
brevo.apikey=
```

Create the file `application-dev.properties` in `dossierfacile-api-owner/src/main/resources`

```properties
mock.storage.path=/path/to/mock_storage/
owner.url=http://localhost:3000
server.port=8083
# SQL
spring.datasource.url=jdbc:postgresql://localhost:5433/dossierfacile
spring.datasource.username=
spring.datasource.password=
# Keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=dossier-facile-api
keycloak.server.client.secret=
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8085/auth/realms/dossier-facile-owner
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8085/auth/realms/dossier-facile-onwer/protocol/openid-connect/certs
tenant.jwt.issuer-uri=http://localhost:8085/auth/realms/dossier-facile
tenant.jwt.jwk-set-uri=http://localhost:8085/auth/realms/dossier-facile/protocol/openid-connect/certs
# Brevo
brevo.apikey=
```

Create the file `application-dev.properties` in `dossierfacile-process-file/src/main/resources`

```properties
mock.storage.path=/path/to/mock_storage/
storage.provider.list=LOCAL
server.port=8088
# SQL
spring.datasource.url= jdbc:postgresql://localhost:5433/dossierfacile
spring.datasource.username=
spring.datasource.password=
# Keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=dossier-facile-api
keycloak.server.client.secret=
# Brevo
brevo.apikey=
```

Create the file `application-dev.properties` in `dossierfacile-api-watermark/src/main/resources`

```properties
server.port=8091
mock.storage.path=/path/to/mock_storage/
storage.provider.list=LOCAL
# SQL
spring.datasource.url=jdbc:postgresql://localhost:5433/dossierfacile
spring.datasource.username=
spring.datasource.password=
```

Create the file `application-dev.properties` in `dossierfacile-pdf-generator/src/main/resources`

```properties
mock.storage.path=/path/to/mock_storage/
storage.provider.list=LOCAL
# SQL
spring.datasource.url= jdbc:postgresql://localhost:5433/dossierfacile
spring.datasource.username=
spring.datasource.password=
# Keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=dossier-facile-api
keycloak.server.client.secret=
# Brevo
brevo.apikey=
```

For each properties file, copy the `brevo.template.*` properties from `application.properties` to `application-dev.properties` and set the correct ids.

## Build

Run `mvn clean install` from the root folder. This will build every module.

## Launch

In each application folder, run

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev,mockOvh
```

## Infrastructure

![Infrastructure diagram](docs/infrastructure_diagram.jpg)

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)
