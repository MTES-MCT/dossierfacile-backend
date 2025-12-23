import importlib.util
import os
import json
import signal
import sys
import logging
from datetime import datetime, timedelta, timezone
import requests
from elasticsearch import Elasticsearch
from dotenv import load_dotenv
from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.interval import IntervalTrigger

# Charge les variables depuis le fichier .env
load_dotenv()

# === Configuration du logger ===
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s: %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# === Global Config ===
ES_HOST = os.getenv("ES_HOST", "")
ES_LOGIN = os.getenv("ES_LOGIN", "")
ES_PASSWORD = os.getenv("ES_PASSWORD", "")
MATTERMOST_WEBHOOK_URL = os.getenv("MATTERMOST_WEBHOOK_URL", "")

# Variables globales pour le scheduler et la connexion ES
scheduler = None
es = None

# === Fonction d'envoi d'alerte ===
def send_mattermost_alert(title, text, color="danger", webhook_url=None):
    """Envoie une alerte via Mattermost webhook"""
    payload = {
        "attachments": [{
            "fallback": title,
            "color": color,
            "pretext": f":warning: *{title}*",
            "text": text,
            "ts": int(datetime.now(timezone.utc).timestamp())
        }]
    }
    try:
        response = requests.post(webhook_url, json=payload, timeout=10)
        if response.status_code != 200:
            logger.error(f"Error sending alert: {response.status_code} - {response.text}")
        else:
            logger.info(f"Alert sent successfully to {webhook_url}")
    except Exception as e:
        logger.exception(f"Exception sending alert: {e}")

# === Création d'une fonction d'alerte avec webhook spécifique ===
def create_alert_function(webhook_key=None):
    """
    Crée une fonction d'alerte configurée avec un webhook spécifique.
    
    Args:
        webhook_key: Nom de la variable d'environnement contenant l'URL du webhook.
                     Si None, utilise le webhook par défaut (MATTERMOST_WEBHOOK_URL).
    
    Returns:
        callable: Fonction d'alerte configurée avec le webhook spécifique
    """
    def alert_function(title, text, color="danger"):
        """Fonction d'alerte avec webhook pré-configuré"""
        webhook_url = MATTERMOST_WEBHOOK_URL
        
        if webhook_key:
            webhook_url = os.getenv(webhook_key, MATTERMOST_WEBHOOK_URL)
        
        send_mattermost_alert(title, text, color, webhook_url)
    
    return alert_function

# === Chargement de la configuration ===
def load_config(config_file="config.json"):
    """
    Charge la configuration JSON depuis le fichier spécifié.
    
    Args:
        config_file: Nom du fichier de configuration (relatif au répertoire du script)
    
    Returns:
        dict: Configuration chargée
    
    Raises:
        FileNotFoundError: Si le fichier de configuration n'existe pas
        json.JSONDecodeError: Si le fichier JSON est invalide
    """
    config_path = os.path.join(os.path.dirname(__file__), config_file)
    try:
        with open(config_path, "r") as f:
            config = json.load(f)
        return config
    except FileNotFoundError:
        logger.error(f"Configuration file not found: {config_path}")
        raise
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in configuration file: {e}")
        raise
    except Exception as e:
        logger.exception(f"Error loading config.json: {e}")
        raise

# === Chargement et extraction du module de règle ===
def load_rule_module(rule_file):
    """
    Charge dynamiquement un module Python depuis un fichier de règle.
    
    Args:
        rule_file: Chemin relatif vers le fichier de règle Python
    
    Returns:
        tuple: (module, module_name) si succès, (None, module_name) sinon
    """
    if not rule_file:
        return None, None
    
    rule_path = os.path.join(os.path.dirname(__file__), rule_file)
    module_name = os.path.splitext(os.path.basename(rule_file))[0]
    
    if not os.path.exists(rule_path):
        logger.error(f"Rule file not found: {rule_path}")
        return None, module_name
    
    try:
        spec = importlib.util.spec_from_file_location(module_name, rule_path)
        if spec is None or spec.loader is None:
            logger.error(f"Could not create spec for module: {module_name}")
            return None, module_name
        
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module, module_name
    except Exception as e:
        logger.exception(f"Error loading rule module {module_name}: {e}")
        return None, module_name

# === Extraction de la fonction function_name depuis un module ===
def get_module_function(module, module_name, function_name):
    """
    Extrait la fonction 'function_name' d'un module chargé.
    
    Args:
        module: Module Python chargé
        module_name: Nom du module (pour les logs)
    
    Returns:
        callable: Fonction 'function_name' si trouvée, None sinon
    """
    if module is None:
        return None
    
    if not hasattr(module, function_name):
        logger.warning(f"[{module_name}] No '{function_name}' function found.")
        return None
    
    function = getattr(module, function_name)
    if not callable(function):
        logger.warning(f"[{module_name}] '{function_name}' attribute exists but is not callable.")
        return None
    
    return function

# === Préparation des arguments pour l'exécution d'une règle ===
def prepare_rule_arguments(alert_config, es_client, start_time, end_time):
    """
    Prépare les arguments de base pour appeler la fonction run d'une règle.
    Crée une fonction d'alerte configurée avec le webhook spécifique de la configuration.
    
    Args:
        alert_config: Configuration de l'alerte (dict)
        es_client: Client Elasticsearch
        start_time: Début de la fenêtre temporelle (datetime)
        end_time: Fin de la fenêtre temporelle (datetime)
    
    Returns:
        list: Liste des arguments de base à passer à la fonction run
    """
    # Récupérer la clé du webhook depuis la configuration
    webhook_key = alert_config.get("webhook")
    
    # Créer une fonction d'alerte avec le webhook spécifique
    alert_function = create_alert_function(webhook_key)
    
    return [
        es_client,
        alert_config.get("index"),
        alert_config.get("error_threshold"),
        alert_config.get("env"),
        alert_config.get("application"),
        start_time,
        end_time,
        alert_function
    ]

# === Ajout des arguments optionnels (webhook) ===
def add_optional_arguments(args, run_function, alert_config):
    """
    Ajoute les arguments optionnels (comme webhook) si la fonction les accepte.
    
    Args:
        args: Liste des arguments de base
        run_function: Fonction run à appeler
        alert_config: Configuration de l'alerte
    
    Returns:
        list: Liste complète des arguments avec les optionnels
    """
    import inspect
    
    sig = inspect.signature(run_function)
    params = list(sig.parameters.keys())
    
    # Si la fonction accepte plus de paramètres que ceux fournis
    if len(params) > len(args):
        webhook_key = alert_config.get("webhook")
        if webhook_key:
            webhook_url = os.getenv(webhook_key, "")
            args.append(webhook_url)
        else:
            args.append(None)
    
    return args

# === Exécution d'une alerte unique ===
def run_single_alert(alert_config):
    """
    Exécute une seule règle d'alerte.
    
    Args:
        alert_config: Configuration de l'alerte (dict)
    """
    global es
    
    # Initialiser la connexion ES si nécessaire
    if es is None:
        es = Elasticsearch(ES_HOST, basic_auth=(ES_LOGIN, ES_PASSWORD))
    
    rule_file = alert_config.get("rule_file")
    if not rule_file:
        logger.warning("No rule_file specified, skipping...")
        return
    
    # Extraire le nom du module pour les logs
    module_name = os.path.splitext(os.path.basename(rule_file))[0]
    
    # Extraire la fréquence de l'alerte
    frequency = alert_config.get("frequency")
    if not frequency:
        logger.error(f"No frequency specified for alert {module_name}")
        return

    now = datetime.now(timezone.utc)
    start_time = now - timedelta(minutes=frequency)
    
    logger.info(f"Running alert: {module_name} (period: {start_time.isoformat()} to {now.isoformat()})")
    
    # Charger le module de règle
    module, _ = load_rule_module(rule_file)
    if module is None:
        return
        
    # Extraire la fonction run
    run_function = get_module_function(module, module_name, "run")
    if run_function is None:
        return
    
    # Exécuter la règle
    try:
        # Préparer les arguments
        args = prepare_rule_arguments(alert_config, es, start_time, now)
        args = add_optional_arguments(args, run_function, alert_config)
        
        # Appeler la fonction run
        run_function(*args)
        logger.info(f"Alert {module_name} completed successfully")
    except Exception as e:
        logger.exception(f"[{module_name}] Failed to execute: {e}")

# === Validation de la configuration d'alerte ===
def validate_alert_config(alert_config, alert_index=None):
    """
    Valide la configuration d'une alerte.
    
    Args:
        alert_config: Configuration de l'alerte (dict)
        alert_index: Index de l'alerte dans la liste (pour les messages d'erreur)
    
    Returns:
        tuple: (is_valid, error_message, module_name)
               - is_valid: True si la configuration est valide
               - error_message: Message d'erreur si invalide, None sinon
               - module_name: Nom du module extrait du rule_file
    """
    rule_file = alert_config.get("rule_file")
    if not rule_file:
        idx_msg = f" #{alert_index + 1}" if alert_index is not None else ""
        return False, f"Alert{idx_msg} has no 'rule_file' specified", None
    
    module_name = os.path.splitext(os.path.basename(rule_file))[0]
    
    # Vérifier que la fréquence est présente et valide
    frequency = alert_config.get("frequency")
    if frequency is None:
        return False, f"Alert '{module_name}' has no 'frequency' specified", module_name
    
    if not isinstance(frequency, (int, float)) or frequency <= 0:
        return False, f"Alert '{module_name}' has invalid frequency '{frequency}' (must be a positive number)", module_name
    
    # Vérifier les champs requis pour l'exécution
    required_fields = ["index", "error_threshold", "env", "application"]
    missing_fields = [field for field in required_fields if alert_config.get(field) is None]
    if missing_fields:
        return False, f"Alert '{module_name}' is missing required fields: {', '.join(missing_fields)}", module_name
    
    return True, None, module_name

# === Création et configuration d'un job d'alerte ===
def create_alert_job(scheduler, alert_config, alert_index):
    """
    Crée et configure un job d'alerte à partir de la configuration JSON.
    
    Args:
        scheduler: Instance du scheduler APScheduler
        alert_config: Configuration de l'alerte (dict)
        alert_index: Index de l'alerte dans la liste (pour générer un ID unique)
    
    Returns:
        dict: Informations sur le job créé avec les clés:
              - job_id: ID unique du job
              - module_name: Nom du module de règle
              - frequency: Fréquence d'exécution en minutes
              - success: True si le job a été créé avec succès
              - error: Message d'erreur si la création a échoué
    """
    # Valider la configuration
    is_valid, error_message, module_name = validate_alert_config(alert_config, alert_index)
    
    if not is_valid:
        return {
            "job_id": None,
            "module_name": module_name or "unknown",
            "frequency": None,
            "success": False,
            "error": error_message
        }
    
    # Extraire les paramètres de la configuration
    frequency = int(alert_config.get("frequency"))
    
    # Créer le trigger avec la fréquence spécifiée
    trigger = IntervalTrigger(minutes=frequency)
    
    # Générer un ID unique pour le job
    job_id = f"alert_{module_name}_{alert_index}"
    
    # Créer le nom du job
    job_name = f"Alert: {module_name} (every {frequency} min)"
    
    try:
        # Ajouter le job au scheduler
        job = scheduler.add_job(
            run_single_alert,
            trigger=trigger,
            args=[alert_config],
            id=job_id,
            name=job_name,
            replace_existing=True
        )
        
        return {
            "job_id": job_id,
            "module_name": module_name,
            "frequency": frequency,
            "success": True,
            "error": None,
            "job": job
        }
    except Exception as e:
        return {
            "job_id": job_id,
            "module_name": module_name,
            "frequency": frequency,
            "success": False,
            "error": f"Failed to create job: {str(e)}"
        }

# === Configuration de tous les jobs d'alerte ===
def setup_alert_jobs(scheduler, alerts_config):
    """
    Configure tous les jobs d'alerte à partir de la configuration.
    
    Args:
        scheduler: Instance du scheduler APScheduler
        alerts_config: Liste des configurations d'alerte
    
    Returns:
        dict: Résumé de la configuration avec les clés:
              - total: Nombre total d'alertes configurées
              - successful: Nombre de jobs créés avec succès
              - failed: Nombre de jobs qui ont échoué
              - jobs: Liste des résultats de création de chaque job
    """
    jobs = []
    successful = 0
    failed = 0
    
    for idx, alert_config in enumerate(alerts_config):
        job_result = create_alert_job(scheduler, alert_config, idx)
        jobs.append(job_result)
        
        if job_result["success"]:
            successful += 1
            logger.info(f"Scheduled alert '{job_result['module_name']}' to run every {job_result['frequency']} minutes")
        else:
            failed += 1
            logger.warning(f"{job_result['error']}")
    
    return {
        "total": len(alerts_config),
        "successful": successful,
        "failed": failed,
        "jobs": jobs
    }

# === Gestion des signaux pour arrêt propre ===
def signal_handler(signum, frame):
    """Gère les signaux pour un arrêt propre du scheduler"""
    logger.info(f"Received signal {signum}, shutting down gracefully...")
    if scheduler:
        scheduler.shutdown()
    sys.exit(0)

# === Point d'entrée principal ===
def main():
    """Configure et démarre le scheduler avec un job par alerte"""
    global scheduler, es
    
    # Initialiser la connexion Elasticsearch
    es = Elasticsearch(ES_HOST, basic_auth=(ES_LOGIN, ES_PASSWORD))
    
    # Vérifier la connexion ES
    try:
        if not es.ping():
            logger.error("Cannot connect to Elasticsearch. Please check your configuration.")
            sys.exit(1)
        logger.info(f"Connected to Elasticsearch at {ES_HOST}")
    except Exception as e:
        logger.exception(f"Failed to connect to Elasticsearch: {e}")
        sys.exit(1)
    
    # Charger la configuration
    try:
        config = load_config()
    except Exception as e:
        logger.exception(f"Failed to load configuration: {e}")
        sys.exit(1)
    
    alerts = config.get("alerts", [])
    
    if not alerts:
        logger.error("No alerts configured in config.json")
        sys.exit(1)
    
    # Configurer les gestionnaires de signaux
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Créer et configurer le scheduler
    scheduler = BlockingScheduler(timezone='UTC')
    
    # Configurer tous les jobs d'alerte
    jobs_summary = setup_alert_jobs(scheduler, alerts)
    
    # Vérifier qu'au moins un job a été créé avec succès
    if jobs_summary["successful"] == 0:
        logger.error("No valid alerts to schedule")
        sys.exit(1)
    
    logger.info(f"Scheduler started with {jobs_summary['successful']} alert job(s).")
    if jobs_summary["failed"] > 0:
        logger.warning(f"{jobs_summary['failed']} alert(s) failed to schedule.")
    logger.info("Press Ctrl+C to stop.")
    
    try:
        scheduler.start()
    except (KeyboardInterrupt, SystemExit):
        logger.info("Scheduler stopped.")

if __name__ == "__main__":
    main()