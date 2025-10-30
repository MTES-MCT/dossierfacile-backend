# Bibliothèque commune (dossierfacile-common-library)

## Description
Bibliothèque partagée contenant les composants communs utilisés par les différents services du projet.

## Composants principaux
- Entités JPA communes
- Services partagés
- Configurations communes
- Ressources utilitaires

## Services de stockage de fichiers

### FileStorageService
Interface principale pour la gestion du stockage de fichiers. Supporte plusieurs fournisseurs de stockage S3.

### Fournisseurs de stockage disponibles

#### 1. LocalMockStorage
- Stockage local pour le développement
- Utilise le système de fichiers local
- Configuration : `mock.storage.path`

#### 2. OvhFileStorageServiceImpl
- Fournisseur OVH historique
- Utilise AWS SDK v1
- Pour la compatibilité avec les anciens déploiements

#### 3. OutscaleFileStorageServiceImpl
- Fournisseur Outscale
- Utilise AWS SDK v1
- Support du stockage S3 compatible Outscale

#### 4. S3FileStorageServiceImpl (Nouveau)
- **Nouveau fournisseur S3 multi-AZ OVH**
- Utilise AWS SDK v2 pour de meilleures performances
- Fonctionnalités avancées :
  - **Gestion par buckets** : Organisation des fichiers dans 5 buckets différents selon leur usage
  - **Chiffrement côté serveur** : Chiffrement automatique AES256 des fichiers
  - **Gestion avancée des erreurs** : Retry automatique et gestion des exceptions
  - **Support du versioning** : Compatible avec le versioning S3

### Configuration des buckets S3

Les buckets sont configurés dans `S3BucketConfig.java` :

| Bucket | Usage | Configuration |
|--------|-------|---------------|
| RAW_FILE | Fichiers bruts uploadés | `s3.bucket.raw.file.name` (défaut: raw-file) |
| RAW_MINIFIED | Fichiers minifiés/compressés | `s3.bucket.raw.minified.name` (défaut: raw-minified) |
| WATERMARK_DOC | Documents avec filigrane | `s3.bucket.watermark.doc.name` (défaut: watermark-doc) |
| FULL_PDF | PDFs complets générés | `s3.bucket.full.pdf.name` (défaut: full-pdf) |
| FILIGRANE | Fichiers FiligraneFacile | `s3.bucket.filigrane.name` (défaut: filigrane) |

### Migration vers le nouveau fournisseur S3

Pour utiliser le nouveau fournisseur S3 (OVH multi-AZ) :

1. Ajouter les dépendances AWS SDK v2 (déjà incluses dans pom.xml)
2. Configurer les propriétés S3 dans votre fichier de configuration
3. Ajouter `S3` à la liste des fournisseurs : `storage.provider.list=S3`

Exemple de configuration complète :

```properties
# Liste des fournisseurs de stockage
storage.provider.list=S3

# Configuration du nouveau fournisseur S3
s3.region=sbg
s3.endpoint.url=https://s3.sbg.io.cloud.ovh.net
s3.access.key=your-access-key
s3.secret.access.key=your-secret-key

# Noms des buckets (optionnel, valeurs par défaut ci-dessous)
s3.bucket.raw.file.name=raw-file
s3.bucket.raw.minified.name=raw-minified
s3.bucket.watermark.doc.name=watermark-doc
s3.bucket.full.pdf.name=full-pdf
s3.bucket.filigrane.name=filigrane
```

## Migrations de base de données

### 202507240000-add-bucket-to-storage-file.xml
- Ajout de la colonne `bucket` dans la table `storage_file`
- Permet de tracker quel bucket S3 est utilisé pour chaque fichier
- Archive les anciennes clés de chiffrement

### 202508260000-new-garbage-collection-strategy.xml
- Création de la table `garbage_sequence` pour la gestion du garbage collector
- Optimisation du nettoyage des fichiers orphelins
- Permet un suivi incrémental des fichiers à supprimer