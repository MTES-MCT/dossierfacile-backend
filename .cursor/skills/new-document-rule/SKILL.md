# Skill: Nouveau type de document (Document IA -> DossierFacile)

## But
Implementer un nouveau type de document dans `dossierfacile-document-analysis` avec regles metier, mapping Document IA et tests, en respectant les conventions projet.

## Quand utiliser ce skill
- Ajout d'un nouveau `DocumentSubCategory`
- Creation d'un nouveau `*RulesValidationService`
- Ajout de nouvelles rules `DocumentRule`
- Adaptation de mapping Document IA pour un nouveau type de document

## Entrees attendues (depuis l'utilisateur)
- Type de document cible (`DocumentSubCategory`)
- Mode de traitement: `single-file` ou `multi-file`
  - `single-file`: un seul document est attendu dans le funnel (ex: avis d'imposition)
  - `multi-file`: plusieurs documents sont attendus dans le funnel (ex: bulletins de salaire)
- Schema d'extraction Document IA (noms des proprietes)
- Regles metier a implementer
- Cas particuliers de validation (`inconclusive`, expiration, name match, etc.)

## Contexte validation (blocking / inconclusive)
- Une regle **blocking** stoppe le pipeline en cas d'erreur: les regles suivantes ne sont plus executees.
- Exemple de pattern: `HasBeenDocumentIAAnalysedBI`.
- Une regle **inconclusive** est utilisee quand les donnees extraites/presentes ne permettent pas de conclure `valid` ou `invalid`.
- Dans ce cas, retourner explicitement un resultat `INCONCLUSIVE` avec un message metier clair.

## Regles d'implementation obligatoires

1. Mapper de traitement
- `single-file`: utiliser `DocumentIAMergerMapper`
- `multi-file`: utiliser `DocumentIAMultiMapper`

2. Service de validation
- Creer un `*RulesValidationService` dedie au type de document
- Nommer la classe avec le type de document
- Ordre obligatoire des rules en tete:
  1. `HasBeenDocumentIAAnalysedBI`
  2. `ClassificationValidatorB`
  3. puis les rules specifiques du document

3. Wiring configuration (obligatoire)
- Mettre a jour `DocumentAnalysisServiceConfiguration`:
  - ajouter le service dans la signature du bean `documentSubCategoryValidatorMap`
  - ajouter `validators.put(DocumentSubCategory.<NOUVEAU_TYPE>, <service>)`
- Mettre a jour `DocumentIAConfig`:
  - ajouter le subcategory dans `hasToSendFileForAnalysis(...)` si IA requise
  - ajuster `getWorkflowIdForDocumentSubCategory(...)` si workflow specifique

4. Regles metier
- Si regle de nom: s'aligner sur `FrenchIdentityCardNameMatch` et `PayslipNameMatch`
- Si regle d'expiration: s'aligner sur `FrenchIdentityCardExpirationRule`
- Si donnees insuffisantes/ambiguite: retourner `INCONCLUSIVE` avec message explicite
- Chaque regle doit etre rattachee a une valeur `DocumentRule` coherente

5. Mapping Document IA
- Utiliser `@DocumentIAField` et les types Java du DTO
- Supporter les cas imbriques si necessaire (`objet`, `liste d'objets`, `listes de strings`)
- Garder la compatibilite avec sanitizer/mappers existants

## Sortie attendue de l'assistant
La reponse doit suivre strictement cet ordre:
1. Checklist d'implementation
2. Decision de cadrage (`single-file` ou `multi-file`, mapper choisi)
3. Plan de fichiers impactes
   - inclure explicitement `DocumentAnalysisServiceConfiguration` et `DocumentIAConfig`
4. Definition du RulesValidationService (ordre des rules exact)
5. Matrice des regles metier (`DocumentRule`, message, valid/invalid/inconclusive)
6. Implementation detaillee
7. Tests ajoutes/adaptes
8. Commandes de verification
9. Risques/hypotheses

## Qualite minimale
- Couvrir en tests: 1 cas nominal, 1 cas invalid, 1 cas limite
- Ajouter des tests `INCONCLUSIVE` quand necessaire
- Eviter les changements transverses inutiles
- Reutiliser les patterns existants avant toute nouvelle abstraction

## Commandes de verification (template)
```zsh
mvn -pl dossierfacile-document-analysis -Dtest=DocumentIASpecificMapperTest test
mvn -pl dossierfacile-document-analysis -Dtest=DocumentAnalysisServiceConfigurationTest test
mvn -pl dossierfacile-document-analysis test
```
