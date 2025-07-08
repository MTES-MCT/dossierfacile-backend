#!/bin/bash

# Supprimer le dossier dist s'il existe déjà, puis le recréer
rm -rf dist
mkdir -p dist/custom-alerts
mkdir -p dist/elastalert

# Fonction pour afficher l'aide
usage() {
  echo "Usage: $0 [build] [deploy --build --username <username> --serverIp <ip>]"
  exit 1
}

# Charger les variables d'environnement depuis .env
load_env() {
  if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
  else
    echo "Erreur : le fichier .env est manquant."
    exit 1
  fi
}

# Fonction pour la construction des règles
build() {

  for file in rules/*; do
    if [ -f "$file" ]; then
      OUTPUT_FILE="dist/elastalert/$(basename "$file")"
      cp "$file" "$OUTPUT_FILE"
      sed -i '' "s|{{mattermost_webhook_elk_alert_url}}|$MATTERMOST_ELK_ALERT|g" "$OUTPUT_FILE"
      sed -i '' "s|{{mattermost_webhook_cdn_alert_url}}|$MATTERMOST_CDN_ALERT|g" "$OUTPUT_FILE"
    fi
  done

  echo "Création d'une archive zip du dossier custom-alerts ..."
  (cd custom-alerts && zip -r ../dist/custom-alerts/custom-alerts.zip . -x ".env")
  pwd

  echo "Build terminé. Les règles ont été compilées."
}

# Fonction pour le déploiement
deploy() {
  local BUILD="$1"

  echo "Déploiement des règles ElastAlert... ${BUILD}"

  if [ "$BUILD" = true ]; then
    echo "Construction des règles ElastAlert..."
    build
  fi

  if [ -z "$SSH_USERNAME" ]; then
    read -p "Entrez le username SSH du serveur : " SSH_USERNAME
  fi

  if [ -z "$SERVER_IP" ]; then
    read -p "Entrez l'IP du serveur : " SERVER_IP
  fi

  echo "Déploiement en cours. Connexion en cours à $SSH_USERNAME@$SERVER_IP..."

  echo "Arrêt du service ElastAlert..."
  ssh "$SSH_USERNAME@$SERVER_IP" "cd /home/$SSH_USERNAME/docker-elk && sudo docker-compose -f docker-compose-elastalert.yml down"

  echo "Suppression des anciens fichiers de règles..."
  ssh "$SSH_USERNAME@$SERVER_IP" "rm -rf /home/$SSH_USERNAME/docker-elk/elastalert/rules/*"

  echo "Transfert des nouveaux fichiers de règles..."
  scp dist/elastalert/* "$SSH_USERNAME@$SERVER_IP:/home/$SSH_USERNAME/docker-elk/elastalert/rules/"

  echo "Suppression de l'ancienne version de custom-alerts..."
  ssh "$SSH_USERNAME@$SERVER_IP" "sudo rm -rf /home/$SSH_USERNAME/custom-alerts/current"

  echo "Transfert du projet custom-alerts..."
  scp dist/custom-alerts/custom-alerts.zip "$SSH_USERNAME@$SERVER_IP:/home/$SSH_USERNAME/custom-alerts"

  echo "Décompression de custom-alerts..."
  ssh "$SSH_USERNAME@$SERVER_IP" "unzip /home/$SSH_USERNAME/custom-alerts/custom-alerts.zip -d /home/$SSH_USERNAME/custom-alerts/current"

  echo "Suppression de l'archive zip de custom-alerts..."
  ssh "$SSH_USERNAME@$SERVER_IP" "rm -rf /home/$SSH_USERNAME/custom-alerts/custom-alerts.zip"

  echo "Création d'un lien symbolique pour le fichier .env"
  ssh "$SSH_USERNAME@$SERVER_IP" "ln -s /home/$SSH_USERNAME/custom-alerts/.env /home/$SSH_USERNAME/custom-alerts/current/.env"

  echo "Redémarrage du service ElastAlert..."
  ssh "$SSH_USERNAME@$SERVER_IP" "cd /home/$SSH_USERNAME/docker-elk && sudo docker-compose -f docker-compose-elastalert.yml up -d"

  echo "Installation des dépendances python..."
  ssh "$SSH_USERNAME@$SERVER_IP" "cd /home/$SSH_USERNAME/custom-alerts && source venv/bin/activate && cd current && pip install -r requirements.txt"

  echo "Déploiement terminé..."
}

load_env

# Vérifier les arguments
ACTION=""
BUILD=false

while [[ "$#" -gt 0 ]]; do
  case $1 in
  build) ACTION="build" ;;
  deploy) ACTION="deploy" ;;
  --build) BUILD=true ;;
  --username)
    SSH_USERNAME="$2"
    shift
    ;;
  --serverIp)
    SERVER_IP="$2"
    shift
    ;;
  *) usage ;;
  esac
  shift
done

# Exécuter l'action appropriée
case $ACTION in
build)
  build
  ;;
deploy)
  deploy "$BUILD"
  ;;
*)
  usage
  ;;
esac
