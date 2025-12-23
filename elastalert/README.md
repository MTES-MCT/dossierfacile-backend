# README - ElastAlert Rules Management Script

## Description

This shell script manages rules for ElastAlert and custom alerts. It provides two main features:
1. **Build**: Generates rule files by replacing dynamic variables (such as the Mattermost webhook URL).
2. **Build**: Generate a Zip file for the custom alerts.
3. **Deploy**: Deploys the generated rules to a remote server and restarts the ElastAlert service.

### Custom Alerts System

The custom alerts system (`custom-alerts/`) is a Python-based monitoring service that:
- Uses **APScheduler** to run alert checks at configurable intervals
- Supports **per-alert frequency** configuration (each alert can run at different intervals)
- Supports **per-alert webhook** configuration (each alert can use a different Mattermost webhook)
- Runs as a **continuous service** (systemd service or standalone Python script)
- Analyzes Elasticsearch logs and sends alerts via Mattermost when thresholds are exceeded

---

## Prerequisites

- **SSH access** to the server where ElastAlert is deployed.
- **Docker** and **docker-compose** configured on the remote server.
- A rule file in the `rules/` folder containing dynamic variables like `{{mattermost_webhook_url}}`.
- An .env file at the root of the project containing the mattermost webhook URL to use in rules.

```
MATTERMOST_ELK_ALERT=https://mattermost.incubateur.net/hooks/xxxx
MATTERMOST_CDN_ALERT=https://mattermost.incubateur.net/hooks/xxxx
```

---

## Usage

### Available commands

1. **Build only**  
   Compiles rule files by replacing dynamic variables, and generating a zip file for custom alerts.

   ```bash
   ./deploy-rules.sh build --mattermost <url>
   ```

   - **--mattermost <url>**: Mattermost webhook URL to inject into the rules.

2. **Full deployment**  
   Optionally compiles the rules and deploys them to the remote server.

   ```bash
   ./deploy-rules.sh deploy --build --mattermost <url> --username <username> --serverIp <ip>
   ```

   - **--build**: (Optional) Compiles the rules before deployment.
   - **--mattermost <url>**: Mattermost webhook URL to inject into the rules.
   - **--username <username>**: SSH username for connecting to the server.
   - **--serverIp <ip>**: IP address of the remote server.

---

### Usage examples

1. **Generate rules only**:
   ```bash
   ./deploy-rules.sh build --mattermost https://mattermost.example.com/hooks/abc123
   ```

2. **Deploy rules with compilation**:
   ```bash
   ./deploy-rules.sh deploy --build --mattermost https://mattermost.example.com/hooks/abc123 --username admin --serverIp 192.168.1.10
   ```

3. **Deploy without recompilation**:
   ```bash
   ./deploy-rules.sh deploy --username admin --serverIp 192.168.1.10
   ```

---

## Detailed process

1. **Build**:
   - Iterates through all files in the `rules/` folder.
   - Replaces the variable `{{mattermost_webhook_url}}` with the provided URL.
   - Saves the generated files in the `dist/elastalert` folder.
   - Creates a zip file of the custom alerts in the `dist/custom_alerts` folder.

2. **Deploy**:
   - Stops the ElastAlert service on the remote server.
   - Deletes old rules from the server.
   - Transfers the new rules from the `dist/elastalert` folder to the server.
   - Restarts the ElastAlert service.
   - Transfers and unzips the custom alerts zip file to the server.
   - Installs Python dependencies for custom alerts.
   - Links the env file to the new script version.
   - **Note**: The custom alerts service should be managed separately (systemd service). The deployment script does not restart it automatically.

## Custom Alerts Configuration

### Configuration File (`custom-alerts/config.json`)

Each alert is configured with the following properties:

```json
{
  "alerts": [
    {
      "rule_file": "rules/api_500_ratio_alert.py",
      "application": "api-tenant",
      "index": "aggregator-logs-*",
      "error_threshold": 0.05,
      "webhook": "MATTERMOST_ELK_ALERT",
      "frequency": 60,
      "env": "prod"
    }
  ]
}
```

**Configuration properties:**
- `rule_file` (required): Path to the Python rule file (relative to `custom-alerts/`)
- `application` (required): Application name to monitor
- `index` (required): Elasticsearch index pattern (supports wildcards)
- `error_threshold` (required): Error threshold (ratio between 0 and 1)
- `webhook` (optional): Environment variable name containing the Mattermost webhook URL. If not specified, uses `MATTERMOST_WEBHOOK_URL` default.
- `frequency` (required): Execution frequency in minutes (must be a positive integer)
- `env` (required): Environment name (prod, dev, etc.)

### Environment Variables

The custom alerts service requires the following environment variables (in `.env` file):

```bash
# Elasticsearch configuration
ES_HOST=https://your-elasticsearch-host:9200
ES_LOGIN=your_username
ES_PASSWORD=your_password

# Mattermost webhooks (default and per-alert)
MATTERMOST_WEBHOOK_URL=https://mattermost.example.com/hooks/default
MATTERMOST_ELK_ALERT=https://mattermost.example.com/hooks/elk-alerts
MATTERMOST_CDN_ALERT=https://mattermost.example.com/hooks/cdn-alerts
```

### Running Custom Alerts

The custom alerts service runs as a continuous Python process using APScheduler:

1. **As a systemd service** (recommended):
   ```bash
   sudo systemctl start custom-alerts
   sudo systemctl status custom-alerts
   ```

2. **Manually**:
   ```bash
   cd /home/ubuntu/custom-alerts/current
   source ../venv/bin/activate
   python monitor_controller.py
   ```

### Key Features

- **Per-alert scheduling**: Each alert runs at its own configured frequency
- **Per-alert webhooks**: Each alert can use a different Mattermost webhook
- **Time window**: Each alert analyzes logs from the last N minutes (where N = frequency)
- **Independent execution**: Errors in one alert don't affect others
- **Graceful shutdown**: Handles SIGINT/SIGTERM signals properly

For more details, see `custom-alerts/README.md`.

---

## Notes

- The `dist/` folder is recreated on each script execution.
- If the `--mattermost` option is not provided, the script will prompt for the URL interactively.
- Rule files must be placed in the `rules/` folder before executing the script.
- **Custom alerts service**: The custom alerts service runs independently from ElastAlert and should be managed as a separate systemd service.
- **Configuration validation**: The custom alerts service validates all alert configurations at startup. Invalid configurations will prevent the service from starting.
