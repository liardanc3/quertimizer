import { useEffect, useMemo, useRef, useState } from 'react';
import { LoadingOverlay, PageLoadFailureState } from '@/shared/ui';
import {
  normalizeDatabaseStatusData,
  normalizeServerLogData,
  normalizeSystemResourceData,
  updateDatabaseNodeConfig,
  type DatabaseStatusData,
  type DatabaseNodeConfigData,
  type ServerLogData,
  type SystemResourceData,
} from '@/shared/api/monitoring-api';
import {
  sendSessionSocketMessage,
  sendSessionSocketMessageIfOpen,
  SESSION_SOCKET_DESTINATION,
  subscribeSessionSocketMessages,
  type SessionSocketMessage,
} from '@/shared/auth/session-socket';
import { useUiText } from '@/shared/config/ui-text';
import './MonitoringPage.css';

type MonitoringSection = 'resources' | 'logs';
type LogLevel = 'debug' | 'info' | 'warn';

const emptySystemResource: SystemResourceData = {
  systemCpuUsagePercent: 0,
  processCpuUsagePercent: 0,
  systemLoadAverage: 0,
  totalMemoryBytes: 0,
  usedMemoryBytes: 0,
  totalDiskBytes: 0,
  usedDiskBytes: 0,
  uptimeSeconds: 0,
};
const emptyDatabaseStatus: DatabaseStatusData = {
  totalWaitingCount: 0,
  totalRunningCount: 0,
  queues: [],
  nodes: [],
  containers: [],
  configs: [],
};
const today = new Date().toISOString().slice(0, 10);
const LOG_SUBSCRIBE_LINE_SIZE = 700;
const LOG_MAX_VISIBLE_LINE_SIZE = 1200;

function formatPercent(value: number) {
  return `${Math.max(0, value).toFixed(1)}%`;
}

function formatBytes(value: number) {
  if (value <= 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let unitIndex = 0;
  let nextValue = value;
  while (nextValue >= 1024 && unitIndex < units.length - 1) {
    nextValue /= 1024;
    unitIndex++;
  }

  return `${nextValue.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function formatDuration(seconds: number) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${days}d ${String(hours).padStart(2, '0')}h ${String(minutes).padStart(2, '0')}m`;
}

function usageRatio(used: number, total: number) {
  return total > 0 ? Math.min(100, Math.max(0, (used / total) * 100)) : 0;
}

function MetricCard({ label, value, detail, ratio }: { label: string; value: string; detail: string; ratio?: number }) {
  return (
    <article className="monitoring-metric-card">
      <span className="monitoring-metric-label">{label}</span>
      <strong className="monitoring-metric-value">{value}</strong>
      <span className="monitoring-metric-detail">{detail}</span>
      {ratio != null ? (
        <span className="monitoring-meter" aria-hidden="true">
          <span style={{ width: `${Math.min(100, Math.max(0, ratio))}%` }} />
        </span>
      ) : null}
    </article>
  );
}

function EditIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M13.8 2.9a1.9 1.9 0 0 1 2.7 2.7l-8.9 8.9-3.6 1 1-3.6 8.8-9Z" />
      <path d="M12.6 4.1 15.3 6.8" />
    </svg>
  );
}

function resolveSocketErrorMessage(message: SessionSocketMessage, fallbackMessage: string) {
  return typeof message.message === 'string' && message.message.trim() !== '' ? message.message : fallbackMessage;
}

export function MonitoringContent() {
  const { text } = useUiText();
  const sections: Array<{ id: MonitoringSection; label: string }> = useMemo(
    () => [
      { id: 'resources', label: text('MONITORING_RESOURCE_SECTION', '서버 리소스') },
      { id: 'logs', label: text('MONITORING_LOG_SECTION', '서버 로그') },
    ],
    [text],
  );
  const [activeSection, setActiveSection] = useState<MonitoringSection>('resources');
  const [systemResource, setSystemResource] = useState<SystemResourceData>(emptySystemResource);
  const [databaseStatus, setDatabaseStatus] = useState<DatabaseStatusData>(emptyDatabaseStatus);
  const [resourceError, setResourceError] = useState<string | null>(null);
  const [databaseStatusError, setDatabaseStatusError] = useState<string | null>(null);
  const [isResourceLoading, setIsResourceLoading] = useState(true);
  const [isDatabaseStatusLoading, setIsDatabaseStatusLoading] = useState(true);
  const [savingDatabaseId, setSavingDatabaseId] = useState<string | null>(null);
  const [editingDatabaseId, setEditingDatabaseId] = useState<string | null>(null);
  const [draftMaxConcurrency, setDraftMaxConcurrency] = useState(1);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [logLevel, setLogLevel] = useState<LogLevel>('info');
  const [logDate, setLogDate] = useState(today);
  const [serverLog, setServerLog] = useState<ServerLogData>({ level: 'info', date: today, exists: false, lines: [] });
  const [logError, setLogError] = useState<string | null>(null);
  const [isLogLoading, setIsLogLoading] = useState(true);
  const logViewerRef = useRef<HTMLPreElement | null>(null);
  const [shouldScrollLogToBottom, setShouldScrollLogToBottom] = useState(false);

  useEffect(() => {
    return subscribeSessionSocketMessages((message) => {
      if (message.type === 'monitoring.resources.result') {
        setSystemResource(normalizeSystemResourceData(message.resource));
        setResourceError(null);
        setIsResourceLoading(false);
        return;
      }

      if (message.type === 'monitoring.resources.error') {
        setResourceError(resolveSocketErrorMessage(message, text('MONITORING_RESOURCE_LOAD_FAIL_MESSAGE', '서버 리소스를 불러오지 못했습니다.')));
        setIsResourceLoading(false);
        return;
      }

      if (message.type === 'monitoring.database-status.result') {
        setDatabaseStatus(normalizeDatabaseStatusData(message.status));
        setDatabaseStatusError(null);
        setIsDatabaseStatusLoading(false);
        return;
      }

      if (message.type === 'monitoring.database-status.error') {
        setDatabaseStatusError(resolveSocketErrorMessage(message, text('MONITORING_DATABASE_STATUS_LOAD_FAIL_MESSAGE', 'DB 상태를 불러오지 못했습니다.')));
        setIsDatabaseStatusLoading(false);
        return;
      }

      if (message.type === 'monitoring.logs.snapshot') {
        setServerLog(normalizeServerLogData(message.log));
        setLogError(null);
        setIsLogLoading(false);
        setShouldScrollLogToBottom(true);
        return;
      }

      if (message.type === 'monitoring.logs.append') {
        const appendedLines = Array.isArray(message.lines) ? message.lines.filter((line): line is string => typeof line === 'string') : [];
        if (appendedLines.length > 0) {
          setServerLog((current) => ({
            ...current,
            exists: true,
            lines: [...current.lines, ...appendedLines].slice(-LOG_MAX_VISIBLE_LINE_SIZE),
          }));
        }
        return;
      }

      if (message.type === 'monitoring.logs.error') {
        setLogError(resolveSocketErrorMessage(message, text('MONITORING_LOG_LOAD_FAIL_MESSAGE', '서버 로그를 불러오지 못했습니다.')));
        setIsLogLoading(false);
      }
    });
  }, [text]);

  useEffect(() => {
    if (activeSection !== 'resources') {
      return;
    }

    let cancelled = false;

    async function requestResources() {
      setResourceError(null);
      try {
        await sendSessionSocketMessage(SESSION_SOCKET_DESTINATION.monitoringResources);
      } catch (error) {
        if (!cancelled) {
          setResourceError(error instanceof Error ? error.message : text('MONITORING_RESOURCE_LOAD_FAIL_MESSAGE', '서버 리소스를 불러오지 못했습니다.'));
          setIsResourceLoading(false);
        }
      }
    }

    void requestResources();
    const timer = window.setInterval(() => {
      void requestResources();
    }, 10000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [activeSection, text]);

  useEffect(() => {
    if (activeSection !== 'resources') {
      return;
    }

    let cancelled = false;

    async function requestDatabaseStatus() {
      setDatabaseStatusError(null);
      try {
        await sendSessionSocketMessage(SESSION_SOCKET_DESTINATION.monitoringDatabaseStatus);
      } catch (error) {
        if (!cancelled) {
          setDatabaseStatusError(error instanceof Error ? error.message : text('MONITORING_DATABASE_STATUS_LOAD_FAIL_MESSAGE', 'DB 상태를 불러오지 못했습니다.'));
          setIsDatabaseStatusLoading(false);
        }
      }
    }

    void requestDatabaseStatus();
    const timer = window.setInterval(() => {
      void requestDatabaseStatus();
    }, 10000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [activeSection, text]);

  useEffect(() => {
    if (activeSection !== 'logs') {
      return;
    }

    let cancelled = false;
    setIsLogLoading(true);
    setLogError(null);
    setServerLog((current) => ({ ...current, level: logLevel, date: logDate, exists: false, lines: [] }));

    sendSessionSocketMessage(SESSION_SOCKET_DESTINATION.monitoringLogsSubscribe, {
      level: logLevel,
      date: logDate,
      size: LOG_SUBSCRIBE_LINE_SIZE,
    }).catch((error) => {
      if (!cancelled) {
        setLogError(error instanceof Error ? error.message : text('MONITORING_LOG_LOAD_FAIL_MESSAGE', '서버 로그를 불러오지 못했습니다.'));
        setIsLogLoading(false);
      }
    });

    return () => {
      cancelled = true;
      sendSessionSocketMessageIfOpen(SESSION_SOCKET_DESTINATION.monitoringLogsUnsubscribe);
    };
  }, [activeSection, logDate, logLevel, text]);

  useEffect(() => {
    if (activeSection !== 'logs' || !shouldScrollLogToBottom) {
      return;
    }

    const animationFrame = window.requestAnimationFrame(() => {
      if (logViewerRef.current) {
        logViewerRef.current.scrollTop = logViewerRef.current.scrollHeight;
      }
      setShouldScrollLogToBottom(false);
    });
    return () => window.cancelAnimationFrame(animationFrame);
  }, [activeSection, serverLog.lines, shouldScrollLogToBottom]);

  async function handleConfigSave(config: DatabaseNodeConfigData, nextMaxConcurrency: number, nextEnabled: boolean) {
    setSavingDatabaseId(config.databaseId);

    try {
      const savedConfig = await updateDatabaseNodeConfig(config.databaseId, {
        enabled: nextEnabled,
        maxConcurrency: nextMaxConcurrency,
      });
      setDatabaseStatus((current) => ({
        ...current,
        configs: current.configs.map((item) => (item.databaseId === savedConfig.databaseId ? savedConfig : item)),
      }));
      setSuccessMessage('수정이 완료되었습니다.');
    } finally {
      setSavingDatabaseId(null);
    }
  }

  useEffect(() => {
    if (successMessage == null) {
      return;
    }

    const timer = window.setTimeout(() => setSuccessMessage(null), 2200);
    return () => window.clearTimeout(timer);
  }, [successMessage]);

  function openConfigEditor(databaseId: string, currentMaxConcurrency: number) {
    setEditingDatabaseId(databaseId);
    setDraftMaxConcurrency(currentMaxConcurrency);
  }

  async function saveConfigEditor(config: DatabaseNodeConfigData, currentEnabled: boolean) {
    await handleConfigSave(config, Math.max(1, draftMaxConcurrency), currentEnabled);
    setEditingDatabaseId(null);
  }

  function renderResourceSection() {
    const memoryRatio = usageRatio(systemResource.usedMemoryBytes, systemResource.totalMemoryBytes);
    const diskRatio = usageRatio(systemResource.usedDiskBytes, systemResource.totalDiskBytes);
    const isInitialMonitoringLoading = isResourceLoading || isDatabaseStatusLoading;
    if (isInitialMonitoringLoading) {
      return (
        <div className="monitoring-initial-loading">
          <LoadingOverlay ariaHidden />
        </div>
      );
    }

    return (
      <div className="monitoring-section-stack">
        {successMessage ? (
          <div className="monitoring-toast" role="status" aria-live="polite">
            {successMessage}
          </div>
        ) : null}
        {resourceError ? <p className="monitoring-inline-error">{resourceError}</p> : null}
        {databaseStatusError ? <p className="monitoring-inline-error">{databaseStatusError}</p> : null}

        <div className="monitoring-resource-body">
          <div className="monitoring-metric-grid">
            <MetricCard label="CPU" value={formatPercent(systemResource.systemCpuUsagePercent)} detail={`Process ${formatPercent(systemResource.processCpuUsagePercent)}`} ratio={systemResource.systemCpuUsagePercent} />
            <MetricCard label="Memory" value={`${formatBytes(systemResource.usedMemoryBytes)} / ${formatBytes(systemResource.totalMemoryBytes)}`} detail={formatPercent(memoryRatio)} ratio={memoryRatio} />
            <MetricCard label="Disk" value={`${formatBytes(systemResource.usedDiskBytes)} / ${formatBytes(systemResource.totalDiskBytes)}`} detail={formatPercent(diskRatio)} ratio={diskRatio} />
            <MetricCard label="Load / Uptime" value={systemResource.systemLoadAverage.toFixed(2)} detail={formatDuration(systemResource.uptimeSeconds)} />
            <MetricCard label="Queue" value={`${databaseStatus.totalWaitingCount}`} detail="대기 작업" />
            <MetricCard label="Running" value={`${databaseStatus.totalRunningCount}`} detail="실행 중" />
            <MetricCard label="Containers" value={`${databaseStatus.containers.length}`} detail="docker node" />
            <MetricCard label="DB Process" value={`${databaseStatus.nodes.reduce((sum, node) => sum + node.effectiveMaxConcurrency, 0)}`} detail="max concurrency" />
          </div>

          <div className="monitoring-database-table">
            {databaseStatus.nodes.map((node) => {
              const config = databaseStatus.configs.find((item) => item.databaseId === node.databaseId);
              const currentMaxConcurrency = config?.maxConcurrency ?? node.effectiveMaxConcurrency;
              const currentEnabled = config?.enabled ?? node.enabled;
              const editableConfig = config ?? {
                databaseId: node.databaseId,
                databaseName: node.databaseName,
                dbmsType: node.dbmsType,
                dbmsLabel: node.dbmsLabel,
                enabled: currentEnabled,
                maxConcurrency: currentMaxConcurrency,
                updatedAt: '',
              };
              const isEditing = editingDatabaseId === node.databaseId;
              const isSaving = savingDatabaseId === node.databaseId;
              return (
                <article key={node.databaseId} className={`monitoring-database-card ${isSaving ? 'is-saving' : ''}`}>
                  <span className={`monitoring-dbms-text is-${node.dbmsType}`}>{node.dbmsLabel}</span>
                  <strong className="monitoring-database-container-name">{node.containerName}</strong>
                  <strong className="monitoring-database-usage">{`${node.runningCount} / ${currentMaxConcurrency}`}</strong>
                  <div className="monitoring-database-card-controls">
                    <button
                      type="button"
                      className="monitoring-database-edit-button"
                      disabled={isSaving}
                      aria-label={`${node.containerName} max concurrency 수정`}
                      onClick={() => openConfigEditor(node.databaseId, currentMaxConcurrency)}
                    >
                      <EditIcon />
                    </button>
                    {isEditing ? (
                      <div className="monitoring-config-popover">
                        <label>
                          <span>max_concurrency</span>
                          <input
                            type="number"
                            name={`max-concurrency-${node.databaseId}`}
                            inputMode="numeric"
                            autoComplete="off"
                            min={1}
                            max={Math.max(1, node.totalPortCount)}
                            value={draftMaxConcurrency}
                            onChange={(event) => setDraftMaxConcurrency(Number.parseInt(event.currentTarget.value, 10) || 1)}
                          />
                        </label>
                        <div className="monitoring-config-actions">
                          <button type="button" onClick={() => setEditingDatabaseId(null)}>취소</button>
                          <button
                            type="button"
                            disabled={isSaving}
                            onClick={() => void saveConfigEditor(editableConfig, currentEnabled)}
                          >
                            저장
                          </button>
                        </div>
                      </div>
                    ) : null}
                  </div>
                  {isSaving ? (
                    <div className="monitoring-database-card-wave" aria-hidden="true">
                      <span className="wave-loading-placeholder is-long" />
                    </div>
                  ) : null}
                </article>
              );
            })}
          </div>
        </div>
      </div>
    );
  }

  function renderLogSection() {
    if (logError) {
      return <PageLoadFailureState className="submit-history-empty-state" message={logError} />;
    }

    if (isLogLoading && !serverLog.exists && serverLog.lines.length === 0) {
      return (
        <div className="monitoring-log-initial-loading">
          <LoadingOverlay ariaHidden />
        </div>
      );
    }

    return (
      <div className="monitoring-section-stack">
        <div className="monitoring-log-toolbar">
          <div className="solve-dbms-tab-row monitoring-log-level-tabs" role="tablist" aria-label="로그 레벨">
            {(['debug', 'info', 'warn'] as LogLevel[]).map((level) => (
              <button
                key={level}
                type="button"
                className={`solve-dbms-tab ${logLevel === level ? 'is-selected' : ''}`}
                role="tab"
                aria-selected={logLevel === level}
                onClick={() => setLogLevel(level)}
              >
                {level.toUpperCase()}
              </button>
            ))}
          </div>
          <input
            className="monitoring-log-date-input"
            type="date"
            name="monitoring-log-date"
            autoComplete="off"
            value={logDate}
            onChange={(event) => setLogDate(event.target.value)}
          />
        </div>

        <pre className="monitoring-log-viewer" ref={logViewerRef}>
          {serverLog.exists
            ? serverLog.lines.join('\n')
            : `${serverLog.date} ${serverLog.level.toUpperCase()} 로그 파일이 없습니다.`}
        </pre>
        {isLogLoading ? <LoadingOverlay ariaHidden /> : null}
      </div>
    );
  }

  return (
    <section className="panel-card admin-monitoring-panel">
      <div className="admin-monitoring-layout">
        <aside className="admin-anomaly-side-nav" aria-label={text('MONITORING_SECTION_NAV_LABEL', '모니터링 섹션')}>
          {sections.map((section) => {
            const isSelected = section.id === activeSection;
            return (
              <button
                key={section.id}
                type="button"
                className={`admin-anomaly-side-nav-item ${isSelected ? 'is-selected' : ''}`}
                onClick={() => setActiveSection(section.id)}
              >
                <strong>{section.label}</strong>
              </button>
            );
          })}
        </aside>

        <div className="admin-monitoring-content">
          {activeSection === 'resources' ? renderResourceSection() : null}
          {activeSection === 'logs' ? renderLogSection() : null}
        </div>
      </div>
    </section>
  );
}
