# dossierfacile-dfc-demo

Démo partenaire de DossierFacile Connect : flux OIDC (bouton de connexion, échange du code, appel
`/dfc/tenant/profile`, logout) et réception des webhooks. Remplace le dépôt PHP `dossierfacile-dfconnect-demo`.

Le module est volontairement autonome (aucune dépendance aux librairies DossierFacile) : il consomme l'API
exactement comme un partenaire externe.

## Lancer en local

Prérequis : Postgres du `docker-compose.dev.yml`, Keycloak local avec le client `dfconnect-demo`, `api-tenant`
sur le port 8090 et une ligne `user_api` pour le partenaire. Le détail est dans [docs/dfc-demo.md](../docs/dfc-demo.md).

```bash
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
mvn -pl dossierfacile-dfc-demo spring-boot:run -Dspring-boot.run.profiles=dev
```

Puis ouvrir http://demo-partenaire-local.dossierfacile.fr:8092 (port 8092, logs dans `logs/dfc-demo.log`).
