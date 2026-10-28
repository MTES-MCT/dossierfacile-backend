# Démo partenaire DossierFacile Connect (`dossierfacile-dfc-demo`)

## 1. Objectif et périmètre

Le module `dossierfacile-dfc-demo` remplace le dépôt [dossierfacile-dfconnect-demo](https://github.com/MTES-MCT/dossierfacile-dfconnect-demo). Il montre aux partenaires le flux DossierFacile Connect (DFC) de bout en bout et sert de consommateur de référence de l'API `/dfc/**` et des webhooks.

Périmètre :
- Bouton de connexion DFC, échange du code contre un token, appel obligatoire à `GET /dfc/tenant/profile`, logout.
- Réception des webhooks (`POST /webhook`, contrôle de l'en-tête `x-api-key`) et affichage des 50 derniers.

Principes :
- **Flux OIDC sans Spring Security** : les appels HTTP du protocole restent lisibles par un partenaire, quelle que soit sa stack.
- **Aucune dépendance aux librairies DossierFacile** : le module consomme l'API comme un partenaire externe (JSON brut, pas de modèle interne). Si l'API DFC change de contrat, c'est ici que ça casse en premier.
- Rendu Thymeleaf minimal, textes français de la démo d'origine conservés.

Le test e2e preprod `Dossier-Facile-Frontend/e2e-tests/cypress/e2e/tenant/partner.cy.ts` pilote cette démo (lien « Se connecter avec DFC », bouton `button.btn-dossierfacile`, consentement `#kc-login`, texte `/dfc/tenant/profile`) : ces sélecteurs et textes font partie du contrat.

## 2. Flux

```
Partenaire (démo)                 Keycloak DossierFacile              api-tenant
   |  0. bouton -> redirection vers /auth?client_id=dfconnect-demo&scope=openid&redirect_uri=.../display
   |-------------------------------------->|
   |      1. login + écran de consentement (scope dfc), retour sur /display?code=xxx
   |<--------------------------------------|
   |  2. POST /token (client_id, client_secret, code, grant_type=authorization_code, redirect_uri)
   |-------------------------------------->|
   |<------ access_token ------------------|
   |  3. GET /dfc/tenant/profile  Authorization: Bearer <access_token>
   |-------------------------------------------------------------------->|
   |<------ profil du locataire (crée le lien tenant/partenaire) ---------|
   |                                      4. webhook POST <url_callback> x-api-key: <clé>
   |<--------------------------------------------------------------------|
   |  RAZ. GET /dfc/tenant/logout  (ferme les sessions Keycloak du locataire)
```

Côté api-tenant, `/dfc/tenant/profile` exige un token **utilisateur** porteur du scope `dfc` et identifie le partenaire par le claim `azp`, qui doit être égal à `user_api.name`. C'est cet appel qui crée le lien (`tenant_userapi`, `apartment_sharing_link` de type `PARTNER`) et déclenche le premier webhook (`CREATED_ACCOUNT`, `COMPLETED_ACCOUNT` ou `VERIFIED_ACCOUNT` selon le statut).

## 3. Routes et templates

| Page PHP d'origine | Route | Template | Rôle |
|---|---|---|---|
| `index.php` | `GET /` | `index.html` | Accueil, deux liens |
| `connection_dfc.php` | `GET /connection-dfc` | `connection-dfc.html` | Bouton de connexion, affiche l'URL d'autorisation |
| `display.php` | `GET /display?code=` | `display.html` | Échange du code, appel profil, affichage du JSON |
| `logout.php` | `GET /logout` | `logout.html` | Appel `/dfc/tenant/logout` avec le token en session |
| `webhook.php` | `POST /webhook` | réponse texte | Réception et stockage du webhook |
| `display_webhook.php` | `GET /display-webhook` | `display-webhook.html` | Tableau des 50 derniers webhooks |
| — | — | `error.html` | Étape en échec, statut et réponse brute de Keycloak ou de l'API |

Les chemins n'ont plus le suffixe `.php` : la redirect URI Keycloak et `user_api.url_callback` doivent pointer sur `/display` et `/webhook`.

Code : `fr.dossierfacile.dfcdemo.oidc.DfcConnectService` (les quatre appels HTTP), `web/*Controller`, `webhook/WebhookLogRepository` (JDBC, table `webhook_log` créée par `schema.sql`).

## 4. Configuration

Port 8092, logs dans `logs/dfc-demo.log`. Propriétés (préfixe `dfc-demo`, mêmes sémantiques que les variables d'environnement de la démo PHP) :

| Propriété | Variable PHP | Rôle |
|---|---|---|
| `dfc-demo.server-base-url` | `SERVER_BASE_URL` | URL publique de la démo, sert à construire `redirect_uri` (`{server}/display`) |
| `dfc-demo.sso-base-url` | `SSO_BASE_URL` | Racine du Keycloak DossierFacile, sans `/auth` |
| `dfc-demo.api-base-url` | `API_BASE_URL` | Racine de l'API tenant |
| `dfc-demo.client-id` | `CLIENT_ID` | Client Keycloak du partenaire = `user_api.name` |
| `dfc-demo.client-secret` | `CLIENT_SECRET` | Secret du client, jamais rendu ni loggé |
| `dfc-demo.webhook-api-key` | `X_API_KEY` | Valeur attendue dans `x-api-key` = `user_api.partner_api_key_callback` |
| `spring.datasource.*` | `DB_*` | Base propre à la démo (table `webhook_log`) |

## 5. Installation locale

1. **Hostname** : api-tenant refuse `localhost` et les IP privées comme cible de webhook (`ValidUrlValidator`) mais fait confiance à `*.dossierfacile.fr`. Ajouter dans `/etc/hosts` :
   ```
   127.0.0.1   demo-partenaire-local.dossierfacile.fr
   ```
2. **Base** : sur le Postgres du `docker-compose.dev.yml`
   ```
   PGPASSWORD=your_very_secure_password psql -h localhost -p 5432 -U dossierfacile -d postgres -c 'CREATE DATABASE dfc_demo;'
   ```
3. **Keycloak** : `Dossier-Facile-Keycloak/setup-dfconnect-demo.sh` crée le client scope `dfc` s'il manque et le client confidentiel `dfconnect-demo` (secret `dfconnect-demo-secret-local`, consentement requis, redirect URIs `http://demo-partenaire-local.dossierfacile.fr:8092/*` et `http://localhost:8092/*`).
4. **Partenaire** sur la base `dossierfacile` (le BO copie un client `dfconnect-template` absent de l'export local, d'où l'insert direct) :
   ```sql
   INSERT INTO user_api (name, name2, site, email, url_callback, partner_api_key_callback, disabled, completed_status_supported)
   SELECT 'dfconnect-demo', 'Démo partenaire', 'http://demo-partenaire-local.dossierfacile.fr:8092',
          'demo-partenaire@dossierfacile.fr', 'http://demo-partenaire-local.dossierfacile.fr:8092/webhook',
          'dfc-demo-webhook-key-local', false, false
   WHERE NOT EXISTS (SELECT 1 FROM user_api WHERE name = 'dfconnect-demo');
   ```
5. **Properties** : `cp dossierfacile-dfc-demo/src/main/resources/application-dev.properties.example dossierfacile-dfc-demo/src/main/resources/application-dev.properties` (valeurs déjà cohérentes avec les étapes 2 à 4).
6. **Lancement** : `mvn -pl dossierfacile-dfc-demo spring-boot:run -Dspring-boot.run.profiles=dev`, puis http://demo-partenaire-local.dossierfacile.fr:8092. Configuration IntelliJ : `.run/DfcDemo.run.xml` (dossier non versionné).

Un token du front locataire ne peut pas être réutilisé : il n'a pas le scope `dfc` et son `azp` n'est pas un `user_api.name`.

## 6. Vérification

Prérequis : Keycloak, api-tenant (8090), BO (8081), front locataire (9002).

1. Se connecter au front locataire (session SSO), puis sur la démo : « Se connecter avec DFC » → bouton → écran de consentement → `/display` affiche le JSON du profil.
2. `/display-webhook` montre le webhook déclenché par la liaison (asynchrone : rafraîchir).
3. Valider ou refuser le dossier dans le BO : une nouvelle ligne apparaît.
4. Logout → `/dfc/tenant/logout` est appelé et la session de la démo est vidée. Une nouvelle connexion ne redemande le consentement qu'après révocation du partage depuis `/partages` du front locataire.
5. Cas négatifs : `curl -X POST .../webhook -H 'x-api-key: wrong' -d '{}'` → 403 ; consentement refusé ou `code` invalide → page d'erreur avec la réponse brute.

Tests unitaires : `mvn -pl dossierfacile-dfc-demo test` (`DfcConnectServiceTest` avec `MockRestServiceServer`, `WebhookControllerTest` en `@WebMvcTest`).

Test e2e en local : depuis `Dossier-Facile-Frontend/e2e-tests`, `yarn cypress run --spec cypress/e2e/tenant/partner.cy.ts --env dfConnectDemoUrl=http://demo-partenaire-local.dossierfacile.fr:8092`.

## 7. Sécurité et limitations connues

- Le `client_secret` ne quitte jamais `DfcDemoProperties` ; la réponse du token endpoint n'est pas loggée.
- L'`access_token` est conservé **en session serveur** (la démo PHP le passait dans l'URL de logout, donc dans l'historique, les logs du routeur et le `Referer`).
- La comparaison de `x-api-key` est en temps constant ; les webhooks sont stockés bruts et affichés échappés (le PHP les injectait tels quels dans la page).
- Pas de paramètre `state` ni de `nonce` dans la requête d'autorisation, par parité avec la démo PHP. Un vrai partenaire doit en ajouter un (protection CSRF du callback).

