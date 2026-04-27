import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  AlertTriangle,
  CheckCircle2,
  KeyRound,
  Loader2,
  Pause,
  RotateCcw,
  SearchCode
} from 'lucide-react';
import './styles.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8081' : '');

const initialForm = {
  environment: 'staging',
  region: 'us-east-2',
  jiraTicketId: '',
  problemStatement: '',
  logs: '',
  repositoryUrls: '',
  logGroups: '',
  jiraToken: '',
  githubToken: '',
  copilotToken: '',
  awsAccessKeyId: '',
  awsSecretAccessKey: '',
  awsSessionToken: '',
  resumeSessionId: ''
};

function buildSessionPayload(form) {
  return {
    environment: form.environment,
    region: form.region,
    input: {
      jira_ticket_id: form.jiraTicketId,
      problem_statement: form.problemStatement,
      logs: form.logs,
      repository_urls: splitLines(form.repositoryUrls),
      log_groups: splitLines(form.logGroups)
    }
  };
}

function buildCredentialPayload(form) {
  return {
    credentials: {
      jira_token: form.jiraToken,
      github_token: form.githubToken,
      copilot_token: form.copilotToken,
      aws_access_key_id: form.awsAccessKeyId,
      aws_secret_access_key: form.awsSecretAccessKey,
      aws_session_token: form.awsSessionToken
    }
  };
}

function splitLines(value) {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean);
}

function App() {
  const [form, setForm] = useState(initialForm);
  const [access, setAccess] = useState(null);
  const [result, setResult] = useState(null);
  const [currentSession, setCurrentSession] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState('');
  const [error, setError] = useState('');

  const canAnalyze = useMemo(() => currentSession && access?.ready, [access, currentSession]);

  useEffect(() => {
    refreshSessions().catch(() => {});
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options
    });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `Request failed: ${response.status}`);
    }
    return response.json();
  }

  async function post(path, body) {
    return request(path, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async function refreshSessions() {
    const response = await request('/analysis-sessions');
    setSessions(response);
  }

  async function createSession() {
    setLoading('session');
    setError('');
    setAccess(null);
    setResult(null);
    try {
      const session = await post('/analysis-sessions', buildSessionPayload(form));
      setCurrentSession(session);
      setForm((current) => ({ ...current, resumeSessionId: session.id }));
      await refreshSessions();
      return session;
    } catch (err) {
      setError(err.message);
      return null;
    } finally {
      setLoading('');
    }
  }

  async function ensureSession() {
    if (currentSession?.id) {
      return currentSession;
    }
    return createSession();
  }

  async function checkAccess() {
    setLoading('access');
    setError('');
    setResult(null);
    try {
      const session = currentSession?.id ? currentSession : await post('/analysis-sessions', buildSessionPayload(form));
      setCurrentSession(session);
      setForm((current) => ({ ...current, resumeSessionId: session.id }));
      const response = await post(`/analysis-sessions/${session.id}/access/check`, buildCredentialPayload(form));
      setAccess(response);
      await refreshSession(session.id);
      await refreshSessions();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function analyze() {
    setLoading('analyze');
    setError('');
    try {
      const session = await ensureSession();
      if (!session) {
        return;
      }
      const response = await post(`/analysis-sessions/${session.id}/analyze`, buildCredentialPayload(form));
      setResult(response);
      await refreshSession(session.id);
      await refreshSessions();
      if (!response.confidence) {
        const accessResponse = await post(`/analysis-sessions/${session.id}/access/check`, buildCredentialPayload(form));
        setAccess(accessResponse);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function pauseSession() {
    if (!currentSession?.id) {
      return;
    }
    setLoading('pause');
    setError('');
    try {
      const session = await post(`/analysis-sessions/${currentSession.id}/pause`, {});
      setCurrentSession(session);
      await refreshSessions();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function resumeSession(sessionId = form.resumeSessionId) {
    if (!sessionId) {
      setError('Enter an analysis session ID to resume.');
      return;
    }
    setLoading('resume');
    setError('');
    setAccess(null);
    setResult(null);
    try {
      await post(`/analysis-sessions/${sessionId}/resume`, {});
      const session = await refreshSession(sessionId);
      hydrateFormFromSession(session);
      await refreshSessions();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function refreshSession(sessionId) {
    const session = await request(`/analysis-sessions/${sessionId}`);
    setCurrentSession(session);
    return session;
  }

  function hydrateFormFromSession(session) {
    setForm((current) => ({
      ...current,
      environment: session.environment || current.environment,
      region: session.region || current.region,
      jiraTicketId: session.input?.jira_ticket_id || '',
      problemStatement: session.input?.problem_statement || '',
      logs: session.input?.logs || '',
      repositoryUrls: (session.input?.repository_urls || []).join('\n'),
      logGroups: (session.input?.log_groups || []).join('\n'),
      resumeSessionId: session.id
    }));
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <h1>Log Analyzer</h1>
          <p>AI-assisted RCA with persisted sessions, checkpoints, and evidence.</p>
        </div>
        <div className="api-pill">API {API_BASE || 'same origin'}</div>
      </header>

      <section className="layout">
        <form className="panel" onSubmit={(event) => event.preventDefault()}>
          <div className="section-heading">
            <SearchCode size={20} />
            <h2>Analysis Request</h2>
          </div>

          <SessionBar
            currentSession={currentSession}
            form={form}
            loading={loading}
            onChange={updateField}
            onCreate={createSession}
            onPause={pauseSession}
            onResume={() => resumeSession()}
          />

          <div className="grid two">
            <label>
              Environment
              <select name="environment" value={form.environment} onChange={updateField}>
                <option value="stable">Stable</option>
                <option value="staging">Staging</option>
                <option value="perf">Perf</option>
                <option value="production">Production</option>
              </select>
            </label>
            <label>
              AWS Region
              <select name="region" value={form.region} onChange={updateField}>
                <option value="us-east-2">US East 2</option>
                <option value="eu-central-1">EU Central 1</option>
                <option value="ap-southeast-2">Sydney ap-southeast-2</option>
              </select>
            </label>
          </div>

          <label>
            Jira Ticket ID
            <input name="jiraTicketId" value={form.jiraTicketId} onChange={updateField} placeholder="APP-1234" />
          </label>

          <label>
            Problem Statement
            <textarea name="problemStatement" value={form.problemStatement} onChange={updateField} rows="4" placeholder="Describe the issue, observed behavior, customer impact, or hypothesis." />
          </label>

          <label>
            Logs
            <textarea name="logs" value={form.logs} onChange={updateField} rows="7" placeholder="Paste relevant application, API, or CloudWatch log lines." />
          </label>

          <div className="grid two">
            <label>
              GitHub Repository URLs
              <textarea name="repositoryUrls" value={form.repositoryUrls} onChange={updateField} rows="4" placeholder="One repository URL per line" />
            </label>
            <label>
              CloudWatch Log Groups
              <textarea name="logGroups" value={form.logGroups} onChange={updateField} rows="4" placeholder="One log group per line" />
            </label>
          </div>

          <div className="section-heading credentials">
            <KeyRound size={20} />
            <h2>Session Access</h2>
          </div>

          <div className="grid two">
            <SecretInput label="Jira Token" name="jiraToken" value={form.jiraToken} onChange={updateField} />
            <SecretInput label="GitHub Token" name="githubToken" value={form.githubToken} onChange={updateField} />
            <SecretInput label="Copilot Token" name="copilotToken" value={form.copilotToken} onChange={updateField} />
            <SecretInput label="AWS Access Key ID" name="awsAccessKeyId" value={form.awsAccessKeyId} onChange={updateField} />
            <SecretInput label="AWS Secret Access Key" name="awsSecretAccessKey" value={form.awsSecretAccessKey} onChange={updateField} />
            <SecretInput label="AWS Session Token" name="awsSessionToken" value={form.awsSessionToken} onChange={updateField} />
          </div>

          <div className="actions">
            <button type="button" className="secondary" onClick={checkAccess} disabled={loading !== ''}>
              {loading === 'access' ? <Loader2 className="spin" size={18} /> : <CheckCircle2 size={18} />}
              Check Access
            </button>
            <button type="button" onClick={analyze} disabled={!canAnalyze || loading !== ''}>
              {loading === 'analyze' ? <Loader2 className="spin" size={18} /> : <SearchCode size={18} />}
              Run RCA
            </button>
          </div>

          {error && <div className="error">{error}</div>}
        </form>

        <aside className="results">
          <AccessPanel access={access} />
          <SessionPanel currentSession={currentSession} sessions={sessions} onResume={resumeSession} />
          <ResultPanel result={result} session={currentSession} />
        </aside>
      </section>
    </main>
  );
}

function SessionBar({ currentSession, form, loading, onChange, onCreate, onPause, onResume }) {
  return (
    <div className="session-bar">
      <div className="session-meta">
        <span>Current Session</span>
        <strong>{currentSession?.id || 'Not created'}</strong>
        {currentSession?.status && <em>{currentSession.status}</em>}
      </div>
      <div className="session-actions">
        <input name="resumeSessionId" value={form.resumeSessionId} onChange={onChange} placeholder="Session ID" />
        <button type="button" className="secondary" onClick={onCreate} disabled={loading !== ''}>
          {loading === 'session' ? <Loader2 className="spin" size={18} /> : <CheckCircle2 size={18} />}
          Create
        </button>
        <button type="button" className="secondary" onClick={onResume} disabled={loading !== ''}>
          {loading === 'resume' ? <Loader2 className="spin" size={18} /> : <RotateCcw size={18} />}
          Resume
        </button>
        <button type="button" className="secondary" onClick={onPause} disabled={!currentSession || loading !== ''}>
          <Pause size={18} />
          Pause
        </button>
      </div>
    </div>
  );
}

function SecretInput({ label, name, value, onChange }) {
  return (
    <label>
      {label}
      <input type="password" name={name} value={value} onChange={onChange} autoComplete="off" />
    </label>
  );
}

function AccessPanel({ access }) {
  return (
    <section className="panel compact">
      <h2>Access Readiness</h2>
      {!access && <p className="muted">Run access check before starting analysis. Tokens are not stored.</p>}
      {access?.checks?.map((check) => (
        <div className={`check ${check.status}`} key={check.name}>
          {check.status === 'missing' ? <AlertTriangle size={18} /> : <CheckCircle2 size={18} />}
          <div>
            <strong>{check.name}</strong>
            <p>{check.message}</p>
          </div>
        </div>
      ))}
    </section>
  );
}

function SessionPanel({ currentSession, sessions, onResume }) {
  return (
    <section className="panel compact">
      <h2>Saved Context</h2>
      {!currentSession && <p className="muted">Create or resume a session to see persisted context.</p>}
      {currentSession && (
        <>
          <div className="result-block">
            <span>Checkpoint</span>
            <p>{currentSession.latest_checkpoint || 'No checkpoint yet.'}</p>
          </div>
          <h3>Steps</h3>
          {(currentSession.steps || []).map((step) => (
            <div className="evidence" key={step.id}>
              <strong>{step.step_type}</strong>
              <p>{step.output_summary}</p>
            </div>
          ))}
          <h3>Copilot LLM Logs</h3>
          {(currentSession.llm_interactions || []).map((interaction) => (
            <div className={`evidence llm ${interaction.status?.toLowerCase()}`} key={interaction.id}>
              <strong>{interaction.provider} / {interaction.model} - {interaction.status}</strong>
              <p>{interaction.response || interaction.error_message}</p>
              <details>
                <summary>Prompt</summary>
                <pre>{interaction.prompt}</pre>
              </details>
            </div>
          ))}
        </>
      )}
      <h3>Recent Sessions</h3>
      {sessions.map((session) => (
        <button type="button" className="session-link" key={session.id} onClick={() => onResume(session.id)}>
          <span>{session.jira_ticket_id || session.id.slice(0, 8)}</span>
          <small>{session.status}</small>
        </button>
      ))}
    </section>
  );
}

function ResultPanel({ result, session }) {
  if (!result && !session?.evidence?.length) {
    return (
      <section className="panel compact">
        <h2>RCA Result</h2>
        <p className="muted">Results and evidence will appear here after the agent runs.</p>
      </section>
    );
  }

  const evidence = result?.evidence || session?.evidence || [];

  return (
    <section className="panel compact">
      <h2>RCA Result</h2>
      {result && (
        <>
          <div className="result-block">
            <span>Summary</span>
            <p>{result.summary}</p>
          </div>
          <div className="result-block">
            <span>Probable Root Cause</span>
            <p>{result.probable_root_cause}</p>
          </div>
          <div className="result-block">
            <span>Impacted Component</span>
            <p>{result.impacted_component}</p>
          </div>
          <div className="result-block">
            <span>Suggested Solution</span>
            <p>{result.suggested_solution}</p>
          </div>
          <div className="confidence">Confidence: {Math.round(result.confidence * 100)}%</div>
        </>
      )}
      <h3>Evidence</h3>
      {evidence.map((item, index) => (
        <div className="evidence" key={`${item.source}-${item.id || index}`}>
          <strong>{item.source} {item.title ? `- ${item.title}` : ''}</strong>
          <p>{item.content || item.detail}</p>
        </div>
      ))}
      {result?.next_actions?.length > 0 && (
        <>
          <h3>Next Actions</h3>
          <ul>
            {result.next_actions.map((action) => (
              <li key={action}>{action}</li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

createRoot(document.getElementById('root')).render(<App />);
