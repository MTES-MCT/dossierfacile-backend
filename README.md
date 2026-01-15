# DossierFacile Back-end

> [!NOTE]
> DossierFacile.fr a été créé par le Ministère de la Transition écologique pour aider à la réalisation de dossiers de location.

The project is available at [DossierFacile.fr](https://dossierfacile.fr).

The front-end code is also accessible in [this repository](https://github.com/MTES-MCT/Dossier-Facile-Frontend).

## Infrastructure

![Infrastructure diagram](docs/infrastructure_diagram.jpg)

## Prerequisites

You need to have [JDK 21](https://openjdk.org/projects/jdk/21/), [maven](https://maven.apache.org/) and [Docker](https://docs.docker.com/engine/install/) installed.

## Docker

In this project we use a Docker Compose to setup the dev environment.
Several services are used:

- PostgreSQL => Database for all the applications
- NGINX => Reverse proxy for the BO to serve SSL
- RabbitMQ => Queue used by pdf-generator, api-tenant and api-watermark

Run:

```shell
docker-compose -f docker-compose.dev.yml up -d
```

To create a dedicated user and database for dossierfacile.

### Linux users

1. Create the file `docker-compose.overwrite.yml`
2. Add the following to it:
   ```yaml
   services:
     nginx:
       extra_hosts:
         - "host.docker.internal:host-gateway"
   ```
3. Add it to the `./.git/info/exclude` file to ignore it without modifying the shared `.gitignore`
4. Run the stack with `docker compose -f docker-compose.dev.yml -f docker-compose.overwrite.yml up -d`

You may also need to add pgcrypt extension to your local database:

1. Connect to the container database `docker exec -it <container_name_or_id> psql -U <username> -d <database_name>`
2. Run this SQL query `CREATE EXTENSION IF NOT EXISTS "pgcrypto";`

## Keycloak

Follow those steps to use [Keycloak](https://www.keycloak.org/) in dev environment locally:

1. Follow the README instructions on repo [Dossier-Facile-Keycloak](https://github.com/MTES-MCT/Dossier-Facile-Keycloak).
2. Connect to your keycloak admin console (default: http://localhost:8085/auth)
3. Create a new realm "dossier-facile"
4. Inside the realm create a new Client scope:
   - Name: dossier
   - Type: Default
   - Display on consent screen: On
   - Include in token scope: On
5. Inside the realm create a new client: "dossier-facile-frontend-localhost"
   - Root: Url of the tenant webProject (default: http://localhost:8090)
   - Home URL: Url of the tenant webProject (default: http://localhost:8090)
   - valid redirect Uris: `*`
   - Valid post logout Uris: `*`
   - web Origins: `*`
   - Client authentication: Off
   - authentication flow: Standard flow and Direct access Grants
   - Theme: df
   - Client scopes: add dossier
6. Create a new realm: "dossier-facile-owner"
7. Inside the realm create a new Client scope:
   - Name: dossier
   - Type: Default
   - Display on consent screen: On
   - Include in token scope: On
8. Inside the realm create a new client: "dossier-facile-owner-localhost"
   - Root: Url of the tenant webProject (default: http://localhost:8090)
   - Home URL: Url of the tenant webProject (default: http://localhost:8090)
   - valid redirect Uris: `*`
   - Valid post logout Uris: `*`
   - web Origins: `*`
   - Client authentication: Off
   - authentication flow: Standard flow and Direct access Grants
   - Theme: df
   - Client scopes: add dossier
9. Inside the realm Master create a new client: "dossier-facile-api"
   - Root: empty
   - Home URL: empty
   - valid redirect Uris: `*`
   - Valid post logout Uris: `*`
   - web Origins: `*`
   - Client authentication: On
   - authentication flow: Standard flow / Direct access Grants / Service account roles
10. In this client you need add a Service account roles
    - In the tab "Service Account Roles" add the role "admin"
11. Save and copy the dossier-facile-api credentials (Client Secret).

## General Config

Create a new folder `mock-storage` to store files.

### Storage Configuration

For development, use the LOCAL provider with mock storage. For production, you can use S3 (OVH Multi-AZ), OVH, or OUTSCALE providers.

Example configuration for the new S3 provider:

```properties
storage.provider.list=S3
s3.region=sbg
s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
s3.access.key=your-access-key
s3.secret.access.key=your-secret-key
```

- Project: [dossierfacile-api-owner](dossierfacile-api-owner/README.md)
- Project: [dossierfacile-api-tenant](dossierfacile-api-tenant/README.md)
- Project: [dossierfacile-api-watermark](dossierfacile-api-watermark/README.md)
- Project: [dossierfacile-bo](dossierfacile-bo/README.md)
- Project: [dossierfacile-pdf-generator](dossierfacile-pdf-generator/README.md)
- Project: [dossierfacile-process-file](dossierfacile-process-file/README.md)
- Project: [dossierfacile-task-scheduler](dossierfacile-task-scheduler/README.md)

## Build

Run `mvn clean install` from the root folder. This will build every module.

## Launch

In each application folder, run

```shell
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)
