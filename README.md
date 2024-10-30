# DossierFacile Back-end

<pre>
DossierFacile.fr a été créé par le Ministère de la Transition écologique
pour aider à la réalisation de dossiers de location.
</pre>

The project is available at [DossierFacile.fr](https://dossierfacile.fr).

The front-end code is also accessible in [this repository](https://github.com/MTES-MCT/Dossier-Facile-Frontend).

## Prerequisites

You need to have [JDK 21](https://openjdk.org/projects/jdk/21/), [maven](https://maven.apache.org/) and [Docker](https://docs.docker.com/engine/install/) installed.

### Database

Run:

```
docker-compose -f docker-compose.dev.yml up -d
```

To create a dedicated user and database for dossierfacile.

### Keycloak

If you want to use [Keycloak](https://www.keycloak.org/) locally, follow the README instructions on repo [Dossier-Facile-Keycloak](https://github.com/MTES-MCT/Dossier-Facile-Keycloak).

To run this project, you will need the realm "dossier-facile" and a new client "dossier-facile-api", with:

- selected theme "df"
- in capability config, "client authentication" activated
  Then go to tab "credentials" and copy the client secret

### Config

Create a new folder `mock_storage` to store files.

Create the file `application-dev.properties` in `dossierfacile-bo/src/main/resources`

```properties
mock.storage.path=/path/to/mock_storage/
server.port=8081
application.name=bo
application.domain=http://localhost:8081
domain.protocol=http
environment=localhost

mock.storage.path=mockstorage
storage.provider.list=LOCAL

# SQL
spring.datasource.url=jdbc:postgresql://localhost:5432/dossierfacile
spring.datasource.username=dossierfacile
spring.datasource.password=

# SSO Google
spring.security.oauth2.client.registration.google.client-id=
spring.security.oauth2.client.registration.google.client-secret=
spring.security.oauth2.client.registration.google.redirect-uri-template={baseUrl}/oauth2/callback/{registrationId}
spring.security.oauth2.client.registration.google.scope=email,profile
authorize.domain.bo=dossierfacile.fr
authorize.bo.access.emails=

# Keycloak
keycloak.server.url=http://localhost:8085/auth
keycloak.server.realm=dossier-facile
keycloak.server.client.id=
keycloak.server.client.secret=

# Logging
logging.config=
logging.level.root=info
logging.file.path=logs
logging.logstash.destination=

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

Note:

- dans le cas du run du service `dossierfacile-bo`, il semble manquer quelques identifiants de templates (notamment côté partner)

## nginx

For dossierfacile-bo, you need https access. You can use this nginx config for that:

```nginx
upstream df-bo-server {
    server localhost:8081;
}

server {
    listen 80;
    listen [::]:80;
    server_name bo-local.dossierfacile.fr;
    location / {
        return 302 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name bo-local.dossierfacile.fr;
    ssl_certificate /etc/ssl/domain.crt;
    ssl_certificate_key /etc/ssl/domain.key;
    ssl_trusted_certificate /etc/ssl/bo_server.crt;
    location / {
        proxy_pass http://df-bo-server/;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
        proxy_redirect default;
    }
    location /static/ {
        alias /static/;
    }
}

```

You can [create the certificate files with open-ssl](https://www.baeldung.com/openssl-self-signed-cert).

Then add the following line to your `/etc/hosts` file:

```
127.0.0.1   bo-local.dossierfacile.fr
```

## Build

Run `mvn clean install` from the root folder. This will build every module.

## Launch

In each application folder, run

```
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```

## Infrastructure

![Infrastructure diagram](docs/infrastructure_diagram.jpg)

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)
