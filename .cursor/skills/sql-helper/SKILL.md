# Skill: SQL helper (modele de donnees PostgreSQL)

## But
Ecrire des requetes SQL fiables sur `dossierfacile` en s'appuyant sur une vision **a jour** du schema, sans devoir relire toutes les migrations a chaque demande.

## Quand utiliser ce skill
- L'utilisateur demande une requete SQL (SELECT / UPDATE / DELETE / CTE / agregations).
- L'utilisateur demande d'ajouter une colonne calculee ou un filtre complexe.
- L'utilisateur demande de verifier la coherence d'une requete avec le modele de donnees.

## Strategie maintenable (source de verite + snapshot)
Utiliser une approche hybride:
1. Source de verite historique: migrations Liquibase dans
   - `dossierfacile-common-library/src/main/resources/db/changelog/databaseChangeLog.xml`
   - `dossierfacile-common-library/src/main/resources/db/migration/`
2. Vue courante exploitable rapidement: un snapshot schema-only PostgreSQL versionne dans le repo.

Cette approche evite:
- de parser des centaines de migrations a chaque task,
- d'oublier des objets hors JPA (views, fonctions SQL, index specifiques),
- les erreurs de colonnes inexistantes.

## Fichiers de reference recommandes
- `docs/sql-helper/schema-current.sql` (snapshot principal)
- `docs/sql-helper/schema-notes.md` (exceptions metier, alias, vues utiles)
- `docs/sql-helper/query-examples.sql` (patterns reutilisables)

## Workflow de mise a jour du schema
Mettre a jour le snapshot quand une migration DB est ajoutee/modifiee.

## Regles d'utilisation du skill
1. Toujours lire d'abord `docs/sql-helper/schema-current.sql`.
2. Si un objet manque/semble obsolete, verifier les derniers fichiers dans `db/migration/`.
3. Si ambiguite persiste, poser une question ciblée avant de proposer une requete destructive.
4. Pour `UPDATE/DELETE`, toujours proposer une version `SELECT` de verification avant execution.
5. Documenter les hypotheses (jointures, cardinalites, colonnes nullable).

## Sortie attendue de l'assistant
La reponse doit suivre cet ordre:
1. Requete SQL proposee
2. Version de verification (SELECT preview) si la requete modifie des donnees
3. Hypotheses sur le schema (tables/colonnes/jointures)
4. Points de vigilance (perf, index, lock, volumetrie)

## Garde-fous SQL
- Eviter `SELECT *` en production.
- Pour suppression massive: exiger un predicat clair + transaction explicite.
- Privilegier des requetes idempotentes quand possible.
- Signaler si un index semble necessaire pour la requete proposee.

## Definition of done pour ce skill
- [ ] `schema-current.sql` existe et est lisible
- [ ] Le snapshot est regenere apres chaque migration importante
- [ ] Les requetes proposees citent explicitement tables/colonnes du snapshot
- [ ] Les requetes destructives incluent un plan de verification/rollback

