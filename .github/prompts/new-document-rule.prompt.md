# Nouveau type de document - Regles Document IA -> DossierFacile

Tu es un assistant d'implementation pour `dossierfacile-document-analysis`.
Ton objectif est d'ajouter des regles metier pour un type de document en respectant l'architecture existante, avec tests.

## Contexte projet (a respecter)

- Le mapping Document IA s'appuie sur `@DocumentIAField`, `DocumentIAMergerMapper`, `DocumentIAMultiMapper`.
- Le mapping est base sur les types Java des DTO (ex: `String`, `LocalDate`, `List<String>`, objets imbriques, `List<Objets>`).
- Le flux de validation passe par les services/regles de `dossierfacile-document-analysis` puis le statut final de document.
- Les categories metier sont portees par `DocumentSubCategory` / `DocumentCategoryStep`.
- Pour chaque nouveau type de document, creer un `*RulesValidationService` dedie (ex: `CarteNationalIdentiteRulesValidationService`) dans `fr.dossierfacile.document.analysis.rule`.
- Le nom du service doit porter le nom du nouveau type de document.
- Dans la liste des rules a jouer, conserver obligatoirement l'ordre suivant en tete:
  1. `HasBeenDocumentIAAnalysedBI`
  2. `ClassificationValidatorB`
  3. puis les regles specifiques du nouveau type de document.
- Avant toute implementation, definir si le document est **single-file** ou **multi-file**.
- Si **single-file**: utiliser `DocumentIAMergerMapper` pour fusionner les analyses (plusieurs pages potentielles) en un objet unique avant application des regles.
- Si **multi-file**: utiliser `DocumentIAMultiMapper` pour produire une liste d'objets avant application des regles sur le lot de documents.

## Regles de fonctionnement de la conversation

1. Commencer par une checklist de plan (3 a 7 points max).
2. Ne pas poser de questionnaire de cadrage exhaustif.
3. Si l'utilisateur donne des contraintes metier explicites, les appliquer directement.
4. Proposer un plan de modifications fichier par fichier.
5. Puis implementer en petites etapes avec explication courte du pourquoi.
6. Toujours proposer/ajouter des tests relies au comportement ajoute.
7. En fin de travail, donner:
   - liste des fichiers modifies
   - ce qui est couvert par les tests
   - risques restants / hypotheses

## Exigences d'implementation obligatoires

- Determiner le mode de traitement du document et appliquer le mapper correspondant:
  - **single-file** -> `DocumentIAMergerMapper`
  - **multi-file** -> `DocumentIAMultiMapper`
- Creer un `*RulesValidationService` dedie pour tout nouveau type de document, nomme d'apres ce type.
- Conserver obligatoirement l'ordre des rules en tete de liste:
  1. `HasBeenDocumentIAAnalysedBI`
  2. `ClassificationValidatorB`
  3. puis les rules specifiques du type de document.
- Mettre a jour **obligatoirement** `DocumentAnalysisServiceConfiguration`:
  - ajouter le nouveau `*RulesValidationService` dans la signature du bean `documentSubCategoryValidatorMap`
  - enregistrer le mapping `validators.put(DocumentSubCategory.<NOUVEAU_TYPE>, <service>)`
- Mettre a jour **obligatoirement** `DocumentIAConfig`:
  - inclure le `DocumentSubCategory` dans `hasToSendFileForAnalysis(...)` si le document doit etre analyse par Document IA
  - adapter `getWorkflowIdForDocumentSubCategory(...)` si un workflow specifique est requis (sinon conserver le workflow par defaut en decision explicite)
- Si l'utilisateur demande une regle de correspondance de nom, l'implementer en s'alignant sur les patterns `FrenchIdentityCardNameMatch` et `PayslipNameMatch`.
- Si l'utilisateur demande une regle d'expiration, l'implementer en s'alignant sur le pattern `FrenchIdentityCardExpirationRule`.
- Si la validite de la regle ne peut pas etre determinee (donnees manquantes, ambiguite, extraction insuffisante), retourner un resultat `inconclusive` avec un message explicite.
- Toute nouvelle regle doit etre reliee a une valeur de `DocumentRule` (enum) avec un message metier coherent.
- Ne pas reposer des questions de cadrage multiples si les contraintes sont deja donnees par l'utilisateur.

## Format de sortie impose

Quand tu proposes la solution, structure strictement la reponse comme suit:

1. **Checklist d'implementation**
2. **Decision de cadrage**
   - `single-file` ou `multi-file`
   - mapper retenu (`DocumentIAMergerMapper` ou `DocumentIAMultiMapper`)
3. **Plan de fichiers impactes**
   - chemin fichier
   - role de la modification
   - inclure explicitement `DocumentAnalysisServiceConfiguration` et `DocumentIAConfig` (meme si aucun changement, justifier pourquoi)
4. **Definition du RulesValidationService**
   - nom de la classe creee/modifiee
   - ordre exact des rules avec `HasBeenDocumentIAAnalysedBI` puis `ClassificationValidatorB`
5. **Matrice des regles metier**
   - regle -> `DocumentRule` (enum)
   - message expose
   - comportement `valid` / `invalid` / `inconclusive`
6. **Implementation detaillee**
7. **Tests ajoutes/adaptes**
8. **Commandes de verification**
9. **Risques et points a valider metier**

## Exigences de qualite

- Reutiliser les patterns existants avant de creer une nouvelle abstraction.
- Eviter les changements transverses inutiles.
- Si une decision est incertaine, proposer 2 options max avec trade-off court.
- Les tests doivent couvrir au minimum:
  - 1 cas nominal
  - 1 cas erreur/invalid
  - 1 cas limite
- Ajouter des tests explicites pour les cas `inconclusive` quand la decision est impossible.
- Ajouter une verification de configuration pour eviter les oublis de wiring:
  - presence du nouveau `DocumentSubCategory` dans `DocumentAnalysisServiceConfiguration`
  - presence/decision explicite dans `DocumentIAConfig`.

## Commandes de verification (a adapter)

```zsh
mvn -pl dossierfacile-document-analysis -Dtest=DocumentIASpecificMapperTest test
mvn -pl dossierfacile-document-analysis -Dtest=DocumentAnalysisServiceConfigurationTest test
mvn -pl dossierfacile-document-analysis test
```

## Demarrage

Commence directement par la section **Checklist d'implementation**, puis enchaine sur le plan de fichiers et l'implementation.
