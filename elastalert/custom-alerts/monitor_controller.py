import importlib.util
import os
import json
from datetime import datetime, timedelta
import requests
from elasticsearch import Elasticsearch
from dotenv import load_dotenv

# Charge les variables depuis le fichier .env
load_dotenv()

# === Global Config ===
ES_HOST = os.getenv("ES_HOST", "")
ES_LOGIN = os.getenv("ES_LOGIN", "")
ES_PASSWORD = os.getenv("ES_PASSWORD", "")
MATTERMOST_WEBHOOK_URL = os.getenv("MATTERMOST_WEBHOOK_URL", "")
TIMEFRAME_MINUTES = 60

es = Elasticsearch(ES_HOST,basic_auth=(ES_LOGIN, ES_PASSWORD))
now = datetime.utcnow()
start_time = now - timedelta(minutes=TIMEFRAME_MINUTES)

# === Fonction d'envoi d'alerte ===
def send_alert(title, text, color="danger"):
    if not MATTERMOST_WEBHOOK_URL:
        print("[DRY RUN] Would send alert:", title, text)
        return
    payload = {
        "attachments": [{
            "fallback": title,
            "color": color,
            "pretext": f":warning: *{title}*",
            "text": text,
            "ts": int(datetime.now().timestamp())
        }]
    }
    response = requests.post(MATTERMOST_WEBHOOK_URL, json=payload)
    if response.status_code != 200:
        print("Error sending alert:", response.text)

# === Charger la config JSON ===
config_path = os.path.join(os.path.dirname(__file__), "config.json")
with open(config_path, "r") as f:
    config = json.load(f)

alerts = config.get("alerts", [])

for alert in alerts:
    rule_file = alert.get("rule_file")
    if not rule_file:
        print("No rule_file specified, skipping...")
        continue

    rule_path = os.path.join(os.path.dirname(__file__), rule_file)
    module_name = os.path.splitext(os.path.basename(rule_file))[0]

    try:
        spec = importlib.util.spec_from_file_location(module_name, rule_path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)

        if hasattr(module, "run"):
            print(f"Running rule: {module_name}")
            module.run(
                es,
                alert.get("index"),
                alert.get("error_threshold"),
                alert.get("env"),
                alert.get("application"),
                start_time,
                now,
                send_alert
            )
        else:
            print(f"[{module_name}] No 'run' function found.")
    except Exception as e:
        print(f"[{module_name}] Failed to execute: {e}")