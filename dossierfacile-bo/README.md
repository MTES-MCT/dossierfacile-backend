# Dossierfacile BO (dossierfacile-bo)

## Description

The back office for the Dossierfacile operators, to validate applications.

## Main Features

- SSO with google
- Display the pending applications
- Review an application
- Validate or refuse an application

## Configuration

Copy [`src/main/resources/application-dev.properties.example`](src/main/resources/application-dev.properties.example) to `src/main/resources/application-dev.properties` (git-ignored, loaded by the `dev` profile) and fill in the `<REPLACE_ME>` values. The example file is the reference for every property this module reads: keep it up to date when adding one.

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
