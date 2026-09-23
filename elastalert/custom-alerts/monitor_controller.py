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
COOLDOWN_MINUTES = 60

STATE_FILE = os.path.join(os.path.dirname(__file__), ".last_alerts.json")
FALLBACK_STATE_FILE = "/tmp/.custom_alerts_state.json"

def load_alert_state():
    for path in (STATE_FILE, FALLBACK_STATE_FILE):
        try:
            if os.path.exists(path):
                with open(path, "r") as f:
                    return json.load(f)
        except Exception as e:
            print(f"Warning: could not load state from {path}: {e}")
    return {}

def save_alert_state(state):
    for path in (STATE_FILE, FALLBACK_STATE_FILE):
        try:
            with open(path, "w") as f:
                json.dump(state, f, indent=2)
            return
        except Exception as e:
            print(f"Warning: could not save state to {path}: {e}")
    print("Error: unable to persist alert state.")

es = Elasticsearch(ES_HOST, basic_auth=(ES_LOGIN, ES_PASSWORD))
now = datetime.utcnow()
start_time = now - timedelta(minutes=TIMEFRAME_MINUTES)
now_ts = int(now.timestamp())
alert_state = load_alert_state()

# === Fonction d'envoi d'alerte ===
def send_alert(title, text, color="danger"):
    if not MATTERMOST_WEBHOOK_URL:
        print("[DRY RUN] Would send alert:", title, text)
        return
    if isinstance(text, list):
        payload = {
            "text": title,
            "attachments": text
        }
    else:
        payload = {
            "text": title,
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
    alert_key = f"{module_name}:{alert.get('application', '')}:{alert.get('env', '')}"

    last_sent_ts = alert_state.get(alert_key, 0)
    elapsed_seconds = now_ts - last_sent_ts
    cooldown_seconds = COOLDOWN_MINUTES * 60

    if elapsed_seconds < cooldown_seconds:
        remaining_minutes = max(1, int((cooldown_seconds - elapsed_seconds) / 60))
        print(f"[{module_name}] Cooldown active for {alert_key} ({remaining_minutes} min remaining). Skipping.")
        continue

    try:
        spec = importlib.util.spec_from_file_location(module_name, rule_path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)

        if hasattr(module, "run"):
            print(f"Running rule: {module_name}")

            def alert_sender(title, text, color="danger", current_key=alert_key):
                send_alert(title, text, color)
                alert_state[current_key] = int(datetime.utcnow().timestamp())
                save_alert_state(alert_state)

            module.run(
                es,
                alert.get("index"),
                alert.get("error_threshold"),
                alert.get("env"),
                alert.get("application"),
                start_time,
                now,
                alert_sender
            )
        else:
            print(f"[{module_name}] No 'run' function found.")
    except Exception as e:
        print(f"[{module_name}] Failed to execute: {e}")