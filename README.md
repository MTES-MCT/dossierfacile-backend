# DossierFacile Back-end

<pre>
DossierFacile.fr a été créé par le Ministère de la Transition écologique
pour aider à la réalisation de dossiers de location.
</pre>

The project is available at [DossierFacile.fr](https://dossierfacile.fr).

The front-end code is also accessible in [this repository](https://github.com/MTES-MCT/Dossier-Facile-Frontend).

## Infrastructure

![Infrastructure diagram](docs/infrastructure_diagram.jpg)

### Storage Providers

The application supports multiple S3-compatible storage providers:

- **LOCAL**: Mock storage for development (stored in `mock-storage` folder)
- **OVH**: Legacy OVH S3 storage using AWS SDK v1
- **OUTSCALE**: Outscale S3 storage using AWS SDK v1
- **S3**: New multi-AZ S3 provider (OVH) using AWS SDK v2 with advanced features

#### New S3 Multi-AZ Provider (OVH)

The new S3 provider introduces several improvements:

- **AWS SDK v2**: Uses the latest AWS SDK v2 for better performance and features
- **Bucket-based storage**: Files are organized across multiple buckets for better management:
  - `RAW_FILE`: Original uploaded files
  - `RAW_MINIFIED`: Compressed/minified versions
  - `WATERMARK_DOC`: Documents with watermarks
  - `FULL_PDF`: Generated complete PDF files
  - `FILIGRANE`: Files for FiligraneFacile service
- **Server-side encryption**: Automatic encryption of files at rest using AES256
- **Enhanced garbage collection**: Improved cleanup strategy for orphaned files

## Prerequisites

You need to have [JDK 21](https://openjdk.org/projects/jdk/21/), [maven](https://maven.apache.org/) and [Docker](https://docs.docker.com/engine/install/) installed.

## Docker

In this project we use a Docker Compose to setup the dev environment.
Several services are used: 

- PostgreSQL => Database for all the applications
- NGINX => Reverse proxy for the BO to serve SSL
- RabbitMQ => Queue used by pdf-generator, api-tenant and api-watermark

Run:

```
docker-compose -f docker-compose.dev.yml up -d
```

To create a dedicated user and database for dossierfacile.

## Keycloak

Follow those steps to use [Keycloak](https://www.keycloak.org/) in dev environment locally : 

    1 - Follow the README instructions on repo [Dossier-Facile-Keycloak](https://github.com/MTES-MCT/Dossier-Facile-Keycloak).
    2 - Connect to your keycloak admin console (default : http://localhost:8085/auth)
    3 - Create a new realm "dossier-facile"
    4 - Inside the realm create a new Client scope : 
        - Name: dossier
        - Type: Default
        - Display on consent screen: On
        - Include in token scope: On
    5 - Inside the realm create a new client : "dossier-facile-frontend-localhost"
        - Root: Url of the tenant webProject (default : http://localhost:8090)
        - Home URL : Url of the tenant webProject (default: http://localhost:8090)
        - valid redirect Uris : *
        - Valid post logout Uris: *
        - web Origins: *
        - Client authentication : Off
        - authentication flow: Standard flow and Direct access Grants
        - Theme : df 
        - Client scopes : add dossier
    6 - Create a new realm "dossier-facile-owner :
    7 - Inside the realm create a new Client scope : 
        - Name: dossier
        - Type: Default
        - Display on consent screen: On
        - Include in token scope: On
    8 - Inside the realm create a new client : "dossier-facile-owner-localhost"
        - Root: Url of the tenant webProject (default : http://localhost:8090)
        - Home URL : Url of the tenant webProject (default: http://localhost:8090)
        - valid redirect Uris : *
        - Valid post logout Uris: *
        - web Origins: *
        - Client authentication : Off
        - authentication flow: Standard flow and Direct access Grants
        - Theme : df 
        - Client scopes : add dossier
    9 - Inside the realm Master create a new client : "dossier-facile-api"
        - Root: empty
        - Home URL : empty
        - valid redirect Uris : *
        - Valid post logout Uris: *
        - web Origins: *
        - Client authentication : On
        - authentication flow: Standard flow / Direct access Grants / Service account roles
    10 - In this client you need add a Service account roles
        - In the tab "Service Account Roles" add the role "admin"

Save and copy the dossier-facile-api credentials (Client Secret).

## General Config 

Create a new folder `mock-storage` to store files.

### Storage Provider Configuration

To configure storage providers, add the following properties to your `application-dev.properties`:

```properties
# Storage provider list - comma-separated list of providers to use
# Options: LOCAL, OVH, OUTSCALE, S3
storage.provider.list=LOCAL

# For LOCAL provider (development)
mock.storage.path=../mock-storage

# For new S3 provider (OVH Multi-AZ)
s3.region=sbg
s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
s3.access.key=your-access-key
s3.secret.access.key=your-secret-key

# S3 Bucket names (optional, defaults are shown)
s3.bucket.raw.file.name=raw-file
s3.bucket.raw.minified.name=raw-minified
s3.bucket.watermark.doc.name=watermark-doc
s3.bucket.full.pdf.name=full-pdf
s3.bucket.filigrane.name=filigrane
```

- Project : [dossierfacile-api-owner](dossierfacile-api-owner/README.md)
- Project : [dossierfacile-api-tenant](dossierfacile-api-tenant/README.md)
- Project : [dossierfacile-api-watermark](dossierfacile-api-watermark/README.md)
- Project : [dossierfacile-bo](dossierfacile-bo/README.md)
- Project : [dossierfacile-pdf-generator](dossierfacile-pdf-generator/README.md)
- Project : [dossierfacile-process-file](dossierfacile-process-file/README.md)
- Project : [dossierfacile-task-scheduler](dossierfacile-task-scheduler/README.md)

## Build

Run `mvn clean install` from the root folder. This will build every module.

## Launch

In each application folder, run

```
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```

## Recent Changes: New S3 Multi-AZ Provider

### Overview

A new S3 storage provider has been added to the application with support for multi-AZ (Availability Zone) deployments on OVH. This update brings several improvements to file storage and management.

### Key Features

#### 1. AWS SDK v2 Migration
- Migrated from AWS SDK v1 to v2 for the new S3 provider
- Better performance and modern API
- Improved error handling and retry mechanisms
- Both SDK versions coexist (v1 for legacy OVH/Outscale, v2 for new S3)

#### 2. Bucket-Based File Organization
Files are now organized across 5 specialized S3 buckets:

| Bucket | Purpose | Used By |
|--------|---------|---------|
| `raw-file` | Original uploaded files | api-tenant, process-file |
| `raw-minified` | Compressed/optimized files | process-file |
| `watermark-doc` | Documents with watermarks | pdf-generator |
| `full-pdf` | Complete generated PDFs | pdf-generator |
| `filigrane` | FiligraneFacile documents | api-watermark |

#### 3. Server-Side Encryption
- All files stored in the new S3 provider are automatically encrypted using AES256
- Encryption is handled server-side by the S3 provider
- No application-level encryption key management required

#### 4. Enhanced Garbage Collection
- New incremental garbage collection strategy
- Uses `garbage_sequence` table to track processing progress
- Optimized SQL queries for better performance
- Supports all storage providers including the new S3 multi-AZ

#### 5. Database Migrations
Two new database migrations have been added:

1. **202507240000-add-bucket-to-storage-file.xml**
   - Adds `bucket` column to `storage_file` table
   - Archives old encryption keys (no longer needed with server-side encryption)

2. **202508260000-new-garbage-collection-strategy.xml**
   - Creates `garbage_sequence` table for efficient garbage collection
   - Initializes sequence tracking for tenant documents

### Migration Guide

#### For Existing Deployments

1. **Update configuration** to include the new S3 provider:
```properties
storage.provider.list=S3,OVH  # Keep OVH for backward compatibility

s3.region=sbg
s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
s3.access.key=your-access-key
s3.secret.access.key=your-secret-key
```

2. **Database migrations** will run automatically via Liquibase

3. **Enable garbage collection** (optional but recommended):
```properties
garbage-collection.enabled=true
garbage-collection.objects-by-iteration=100
garbage-collection.seconds-between-iterations=60
```

#### For New Deployments

Simply configure the S3 provider from the start:
```properties
storage.provider.list=S3

s3.region=sbg
s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
s3.access.key=your-access-key
s3.secret.access.key=your-secret-key
```

### Breaking Changes

- **BackupFilesTask removed**: The backup task for file replication has been removed as the new S3 multi-AZ provider handles replication natively
- Legacy storage providers (OVH, OUTSCALE) continue to work for backward compatibility

### Performance Improvements

- **Optimized SQL queries** in garbage collection reduce database load
- **Incremental processing** avoids re-scanning previously processed files
- **Batch processing** limits handled in all storage operations
- **Server-side encryption** offloads encryption overhead to S3

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)
