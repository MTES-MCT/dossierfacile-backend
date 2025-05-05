# README - ElastAlert Rules Management Script

## Description

This shell script manages rules for ElastAlert. It provides two main features:
1. **Build**: Generates rule files by replacing dynamic variables (such as the Mattermost webhook URL).
2. **Build**: Generate a Zip file for the custom alerts.
3. **Deploy**: Deploys the generated rules to a remote server and restarts the ElastAlert service.

---

## Prerequisites

- **SSH access** to the server where ElastAlert is deployed.
- **Docker** and **docker-compose** configured on the remote server.
- A rule file in the `rules/` folder containing dynamic variables like `{{mattermost_webhook_url}}`.

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
   - Create a zip file of the custom alerts in the `dist/custom_alerts` folder.

2. **Deploy**:
   - Stops the ElastAlert service on the remote server.
   - Deletes old rules from the server.
   - Transfers the new rules from the `dist/elastalert` folder to the server.
   - Restarts the ElastAlert service.
   - Transfers and unzip the custom alerts zip file to the server.
   - Install python dependencies for custom alerts.
   - Link the env file to the new script version.

---

## Notes

- The `dist/` folder is recreated on each script execution.
- If the `--mattermost` option is not provided, the script will prompt for the URL interactively.
- Rule files must be placed in the `rules/` folder before executing the script.
