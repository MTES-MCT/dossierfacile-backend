# Dossierfacile BO (dossierfacile-bo)

## Description
The back office for the Dossierfacile operators, to validate applications.

## Main Features
- SSO with google
- Display the pending applications
- Review an application
- Validate or refuse an application

## Configuration
Create a file `application-dev.properties` in `dossierfacile-api-bo/src/main/resources`

```properties
# Port for this Application 
server.port=8081
application.name=bo
environment=localhost

# Path for the mock storage
mock.storage.path=../mock-storage
# List of the providers to use for the storage
storage.provider.list=LOCAL

#URL of the database
spring.datasource.url=
#Username of the database
spring.datasource.username=
#Password of the database
spring.datasource.password=

# SSO Google (You can get the Google client-id and the google-client-secret on the vault warden)
spring.security.oauth2.client.registration.google.client-id=
spring.security.oauth2.client.registration.google.client-secret=
spring.security.oauth2.client.registration.google.redirect-uri-template={baseUrl}/oauth2/callback/{registrationId}
spring.security.oauth2.client.registration.google.scope=email,profile
# Allow the connection from every one who has a dossierfacile.fr email
authorize.domain.bo=dossierfacile.fr
# Allow the connection to specific emails (used in preprod)
authorize.bo.access.emails=

# Control the max number of application that can be processed by day by operator (default: 600)
process.max.dossier.by.day=
# Control the max number of application that can be processed by interval by operator (default : 20)
process.max.dossier.by.interval=
# Interval to apply the control (default : 10)
process.max.dossier.time.interval=
# Time to wait before a selected application can be process by another operator
time.reprocess.application.minutes=
# Set the number of days to display in operator's dashboard (default :5)
dashboard.days.before.to.display=

# Set the lifespan of the client secret sent to a partner by email
display.client.secret.expiration.delay=

#Keycloak configuration
#Keycloak Url
keycloak.server.url=http://localhost:8085/auth
#Keycloak Realm
keycloak.server.realm=dossier-facile
#Keycloak Client Id for admin purpose => need to be inside the realm Master 
keycloak.server.client.id=dossier-facile-api
#Keycloak secret that need to be retrieved from keycloak. 
keycloak.server.client.secret=

#Brevo config
# Leave this empty for dev
brevo.apikey=
brevo.template.id.message.notification=
brevo.template.id.message.notification.with.details=
brevo.template.id.message.notification.with.partner.and.details=
brevo.template.id.account.deleted=
brevo.template.id.dossier.validated=
brevo.template.id.dossier.fully.validated=
brevo.template.id.dossier.tenant.denied=
brevo.template.id.dossier.tenant.denied.with.details=
brevo.template.id.dossier.tenant.denied.with.partner.and.details=
brevo.template.id.message.notification.with.partner=
brevo.template.id.dossier.fully.validated.with.partner=
brevo.template.id.dossier.tenant.denied.with.partner=
brevo.template.id.dossier.validated.with.partner=
```

**Important**: This step is crucial because Google SSO is configured with this specific redirect URI. Omitting this will result in a `redirect_uri_mismatch` error during login: `Erreur 400 : redirect_uri_mismatch`

### HTTPS config for backOffice access

The `dossierfacile-bo` service requires HTTPS access for Google Single Sign-On (SSO). The `docker-compose.dev.yml` deploys an `nginx` container as a reverse proxy, with configuration located at `./.nginx/nginx.conf`. DossierFacile back-office will be served at https://bo-local.dossierfacile.fr/

### Generate Self-Signed SSL Certificate

Create SSL certificate files using OpenSSL:
```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout nginx.key -out nginx.crt
```

Certificates must be placed in folder `./.nginx/certs`

**Note**: When prompted, fill in the certificate details. The Common Name (CN) should match `bo-local.dossierfacile.fr`.

### Configure local hosts
Add the following line to `/etc/hosts`:
```
127.0.0.1   bo-local.dossierfacile.fr
```

### Initial login and user setup

Log in with a Google account. This automatically creates a user in the `public.user_account` table of the PostgreSQL `dossierfacile` database.

List existing users to find your user ID:
```sql
SELECT *
FROM public.user_account;
```

Add role entry to grant back-office access:
```sql
INSERT INTO public.user_roles
("role", user_id)
VALUES(2, <YOUR_USER_ID>);
```

## LogStash :

For the dev environment the appender Logstash is disabled by default.

## Database :
⚠️ The database is managed by the project dossierfacile-api-tenant.

## Run the application

```shell
    mvn spring-boot:run -D mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```
