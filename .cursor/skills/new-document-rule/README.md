# new-document-rule (Cursor Skill)

Ce skill est la version Cursor du prompt GitHub Copilot:
- source: `.github/prompts/new-document-rule.prompt.md`
- skill: `.cursor/skills/new-document-rule/SKILL.md`

## Usage recommande
1. Ouvrir ta task dans Cursor (ou agent compatible skills)
2. Charger ce skill comme contexte principal
3. Fournir:
   - le `DocumentSubCategory` cible
   - `single-file` ou `multi-file`
     - `single-file`: un seul document attendu dans le funnel (ex: avis d'imposition)
     - `multi-file`: plusieurs documents attendus dans le funnel (ex: bulletins de salaire)
   - schema Document IA attendu
   - regles metier souhaitees

## Rappel important
Le skill force explicitement la mise a jour de:
- `DocumentIAConfig`
- `DocumentAnalysisServiceConfiguration`

pour eviter les oublis de wiring observes sur les impl precedentes.

## Contexte sur les regles
- Une regle **blocking** bloque le process en cas d'erreur: les regles suivantes ne sont plus executees (ex: `HasBeenDocumentIAAnalysedBI`).
- Une regle **inconclusive** est emise quand les donnees extraites/presentes ne suffisent pas a trancher `valid` ou `invalid`; on retourne alors explicitement une valeur `INCONCLUSIVE`.
