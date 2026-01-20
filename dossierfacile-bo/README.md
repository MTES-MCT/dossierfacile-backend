# Dossierfacile BO (dossierfacile-bo)

## Description

The back office for the Dossierfacile operators, to validate applications.

## Main Features

- SSO with google
- Display the pending applications
- Review an application
- Validate or refuse an application

## Configuration

Create a file `application-dev.properties` in `dossierfacile-bo/src/main/resources`

```properties
# Port for this Application
server.port=8081
application.name=bo
environment=localhost

tenant.base.url=http://localhost:9002
# Path for the mock storage
mock.storage.path=../mock-storage
# List of the providers to use for the storage
storage.provider.list=LOCAL

# e2e tests
testing.api.allowed-tenant-email=ywiwyne-1268@yopmail.com

# TODO: replace with your database credentials if changed in root docker-compose-dev.yml
# URL of the database
spring.datasource.url=jdbc:postgresql://localhost:5432/dossierfacile
# Username of the database
spring.datasource.username=dossierfacile
# Password of the database
spring.datasource.password=your_very_secure_password

# Keycloak configuration
# Keycloak Url
keycloak.server.url=http://localhost:8085/auth
# Keycloak Realm
keycloak.server.realm=dossier-facile-bo
# Keycloak Client Id for admin purpose => need to be inside the realm Master
keycloak.server.client.id=dossier-facile-bo
# Keycloak secret that need to be retrieved from `keycloak > master realm > Clients > dossier-facile-api > Credentials > Client Secret`
keycloak.server.client.secret=<REPLACE_ME_KEYCLOAK_SECRET>

# SSO Keycloak
spring.security.oauth2.client.provider.keycloak.issuer-uri=${keycloak.server.url}/realms/${keycloak.server.realm}
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.client-id=${keycloak.server.client.id}
spring.security.oauth2.client.registration.keycloak.client-secret=${keycloak.server.client.secret}
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.client-name=Keycloak

# Allow the connection from every one who has a dossierfacile.fr email
authorize.domain.bo=dossierfacile.fr
# Allow the connection to specific emails (used in preprod)
authorize.bo.access.emails=

# Leave this commented for dev
# Control the max number of application that can be processed by day by operator (default: 600)
#process.max.dossier.by.day=
# Control the max number of application that can be processed by interval by operator (default: 20)
#process.max.dossier.by.interval=
# Interval to apply the control (default: 10)
#process.max.dossier.time.interval=
# Set the number of days to display in operator's dashboard (default: 5)
#dashboard.days.before.to.display=
# Time to wait before a selected application can be process by another operator (no default value).
#time.reprocess.application.minutes=5

# Set the lifespan of the client secret sent to a partner by email
display.client.secret.expiration.delay=

# Brevo config
# Leave this empty for dev
brevo.apikey=
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
brevo.template.id.partner.access.revoked=
```

**Important**: This step is crucial because Google SSO is configured with this specific redirect URI. Omitting this will result in a `redirect_uri_mismatch` error during login: `Erreur 400: redirect_uri_mismatch`

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

## LogStash

For the dev environment the appender Logstash is disabled by default.

## Database

⚠️ The database is managed by the project dossierfacile-api-tenant.

## Run the application

```shell
mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```

## Configure keycloak for sso with pro-connect

1. Create a Realm "dossier-facile-bo"
2. Create a client "dossier-facile-bo"
3. Root URL: https://bo-local.dossierfacile.fr
4. Home URL: https://bo-local.dossierfacile.fr
5. Valid redirect Uris: `*`
6. Valid post logout redirect Uris: `*`
7. Web origins: `*`
8. Capability config:
   - Client authentication: ON
   - Authorization: OFF
   - Authentication flow: Standard Flow, Implicit Flow, Direct access grants
9. Logout settings:
   - Front channel logout: ON

You can now create a user, validate his email and set a password. You can connect through keycloak and add rights in the database for your user.

### Add Pro connect

1. Create a basic flow "Authentication Flow" named "Pro connect first broker login simple"
   - Add Step "Review Profile" with requirement "REQUIRED"
   - Add sub flow "Pro connect User creation or linking" with requirement "REQUIRED"
   - add sub step "Create User if Unique" with requirement "ALTERNATIVE"
   - add sub flow "Pro connect handle existing account" with requirement "Alternative"
   - add sub step "Automatically set existing user" with requirement "Alternative"
2. Create a new identity provider: "Agentconnect" with the alias: "pro-connect"
3. On the portal: https://partenaires.proconnect.gouv.fr/apps create a new app:
   - redirect uri: <KEYCLOAK_SERVER_URL>/auth/realms/<KEYCLOAK_SERVER_REALM>/broker/<IDP_ALIAS>/endpoint
   - logout_uri: <KEYCLOAK_SERVER_URL>/auth/realms/<KEYCLOAK_SERVER_REALM>/broker/<IDP_ALIAS>/endpoint/logout_response
   - save clientId and clientSecret
4. On the identity provider set the clientId and clientSecret
5. For the environment AgentConnect use: "INTEGRATION_INTERNET"
6. Advanced settings:
   - scopes: "openid given_name usual_name email uid"
   - store tokens: ON
   - Accepts prompt=none forward from client = ON
   - Disable user info: OFF
   - Trust Email: ON
   - Account linking only: OFF
   - Hide on login page: OFF
   - Verify essential claim: OFF
   - First login flow override: "Pro connect first broker login simple"
   - Mappers:
     - Name: "email"
       - "Sync: Force"
       - "Mapper type: Attribute Importer"
       - "Claim: email"
       - "User attribute: email"
     - Name: "lastName"
       - "Sync: Force"
       - "Mapper type: Attribute Importer"
       - "Claim: usual_name"
       - "User attribute: lastName"
     - Name: "provider"
       - "Sync: Force"
       - "Mapper type: Hardcoded Attribute"
       - "User attribute: provider"
       - "User attribute value: PRO_CONNECT"
     - Name: "pro-connect"
       - "Sync: Force"
       - "Mapper type: Hardcoded Attribute"
       - "User attribute: pro-connect"
       - "User attribute value: true"
7. On the keycloak login page you should now see the button "pro-connect"
