#!/bin/bash

# Supprimer le dossier dist s'il existe déjà, puis le recréer
rm -rf dist
mkdir -p dist/custom-alerts
mkdir -p dist/elastalert

# Fonction pour afficher l'aide
usage() {
  echo "Usage: $0 [build --mattermost <url>] [deploy --build --mattermost <url> --username <username> --serverIp <ip>]"
  exit 1
}

# Fonction pour la construction des règles
build() {
  local MATTERMOST_URL="$1"
  if [ -z "$MATTERMOST_URL" ]; then
    read -p "Entrez l'URL Mattermost : " MATTERMOST_URL
  fi

  for file in rules/*; do
    if [ -f "$file" ]; then
      perl -pe "s/{{mattermost_webhook_url}}/$(printf '%s' "$MATTERMOST_URL" | sed 's/[&/\]/\\&/g')/g" "$file" > "dist/elastalert/$(basename "$file")"
    fi
  done

  echo "Création d'une archive zip du dossier custom-alerts ..."
  (cd custom-alerts && zip -r ../dist/custom-alerts/custom-alerts.zip . -x ".env")
  pwd

  echo "Build terminé. Les règles ont été compilées."
}

# Fonction pour le déploiement
deploy() {
  local MATTERMOST_URL="$1"
  local BUILD="$2"

  if [ "$BUILD" = true ]; then
    if [ -z "$MATTERMOST_URL" ]; then
      echo "Erreur : L'option --mattermost <url> est requise avec --build."
      exit 1
    fi
    build "$MATTERMOST_URL"
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
  ssh "$SSH_USERNAME@$SERVER_IP" "rm -rf /home/$SSH_USERNAME/custom-alerts/current"

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

# Vérifier les arguments
ACTION=""
MATTERMOST_URL=""
BUILD=false

while [[ "$#" -gt 0 ]]; do
  case $1 in
    build) ACTION="build" ;;
    deploy) ACTION="deploy" ;;
    --mattermost) MATTERMOST_URL="$2"; shift ;;
    --build) BUILD=true ;;
    --username) SSH_USERNAME="$2"; shift ;;
    --serverIp) SERVER_IP="$2"; shift ;;
    *) usage ;;
  esac
  shift
done

# Exécuter l'action appropriée
case $ACTION in
  build)
    build "$MATTERMOST_URL"
    ;;
  deploy)
    deploy "$MATTERMOST_URL" "$BUILD"
    ;;
  *)
    usage
    ;;
esac