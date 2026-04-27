# Log Analyzer

AI-assisted log analysis tool for Jira, AWS CloudWatch, GitHub, and Copilot-backed root cause analysis.

## Modules

- `frontend`: React UI source for analysis input, credential checks, and RCA results.
- `integration-service`: Spring Boot MVC application that serves the compiled React UI and backend APIs from one jar.

## V1 Scope

- Session-only credential entry.
- Environment and AWS region selection.
- Input by Jira ticket, problem statement, logs, or a mix of all three.
- Access readiness validation before analysis.
- Stubbed adapters for Jira, CloudWatch, GitHub, and Copilot.
- Structured RCA result with evidence, probable cause, suggested fix, and next actions.

## Configuration

Set long-lived local configuration through environment variables or update `integration-service/src/main/resources/application.yml`.
Tokens entered in the UI are accepted for the current request only and are not persisted.

For WSL/local development, add values to `~/.bashrc` or `~/.zshrc`:

```bash
export JIRA_BASE_URL="https://your-company.atlassian.net"
export JIRA_TOKEN=""
export GITHUB_TOKEN=""
export COPILOT_TOKEN=""
export COPILOT_API_URL="https://your-copilot-compatible-gateway.example.com/v1/chat/completions"
export COPILOT_MODEL="gpt-4.1"
export COPILOT_PROVIDER="copilot-compatible"
export AWS_ACCESS_KEY_ID=""
export AWS_SECRET_ACCESS_KEY=""
export AWS_SESSION_TOKEN=""
export AWS_REGION="us-east-2"
```

Reload the shell:

```bash
source ~/.bashrc
```

The app stores resumable RCA context in a local H2 database at `integration-service/data/log-analyzer`.
Stored context includes analysis sessions, tool steps, evidence, checkpoints, and Copilot LLM prompt/response logs. It does not store credentials.

`COPILOT_API_URL` must point to your organization's Copilot-compatible chat-completions gateway. The application sends an OpenAI-style request body with `model` and `messages`, and stores the prompt/response/error as an `LlmInteraction` for resume and audit.

## Run

### Full Spring Boot App

Maven builds React and copies `frontend/dist` into the Spring Boot static resources.

```bash
cd integration-service
JIRA_BASE_URL=https://your-company.atlassian.net mvn spring-boot:run
```

Open `http://localhost:8081`.

Build a runnable jar:

```bash
cd integration-service
mvn package
java -jar target/integration-service-0.1.0.jar
```

### Frontend Dev Mode

Use this only when actively editing the React UI. The dev server proxies no files into Spring Boot; production-style serving happens from the Spring Boot build.

```bash
cd frontend
npm install
npm run dev
```
