# Bibliothèque commune (dossierfacile-common-library)

## Description
Bibliothèque partagée contenant les composants communs utilisés par les différents services du projet.

## Composants principaux
- Entités JPA communes
- Services partagés
- Configurations communes
- Ressources utilitaires

## Services de stockage

Cette bibliothèque fournit plusieurs implémentations de stockage de fichiers :
- **LocalMockStorage** : Stockage local pour le développement
- **OvhFileStorageServiceImpl** : Fournisseur OVH (legacy)
- **OutscaleFileStorageServiceImpl** : Fournisseur Outscale
- **S3FileStorageServiceImpl** : Nouveau fournisseur S3 multi-AZ (OVH) avec AWS SDK v2, organisation par buckets et chiffrement automatique