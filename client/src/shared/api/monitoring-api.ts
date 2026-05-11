import { getApiBaseUrl } from '@/shared/api/auth-api';
import { createApiErrorFromResponse, getUiTextValue } from '@/shared/config/ui-text';

interface SystemResourceResponse {
  systemCpuUsagePercent?: number;
  processCpuUsagePercent?: number;
  systemLoadAverage?: number;
  totalMemoryBytes?: number;
  usedMemoryBytes?: number;
  totalDiskBytes?: number;
  usedDiskBytes?: number;
  uptimeSeconds?: number;
}

interface JudgeRuntimeQueueResponse {
  dbmsType?: string;
  dbmsLabel?: string;
  waitingCount?: number;
}

interface JudgeRuntimeNodeResponse {
  databaseId?: string;
  databaseName?: string;
  dbmsType?: string;
  dbmsLabel?: string;
  runnerContainer?: string;
  enabled?: boolean;
  ready?: boolean;
  configuredMaxConcurrency?: number;
  effectiveMaxConcurrency?: number;
  runningCount?: number;
  availableRunnerCount?: number;
  totalPortCount?: number;
  availablePortCount?: number;
}

interface DockerContainerResponse {
  name?: string;
  status?: string;
  cpuPercent?: string;
  memoryUsage?: string;
}

interface JudgeConfigResponse {
  databaseId?: string;
  databaseName?: string;
  dbmsType?: string;
  dbmsLabel?: string;
  enabled?: boolean;
  maxConcurrency?: number;
  updatedAt?: string;
}

interface DbRuntimeResponse {
  totalWaitingCount?: number;
  totalRunningCount?: number;
  queues?: JudgeRuntimeQueueResponse[];
  nodes?: JudgeRuntimeNodeResponse[];
  containers?: DockerContainerResponse[];
  configs?: JudgeConfigResponse[];
}

interface ServerLogResponse {
  level?: string;
  date?: string;
  exists?: boolean;
  lines?: string[];
}

export interface SystemResourceData {
  systemCpuUsagePercent: number;
  processCpuUsagePercent: number;
  systemLoadAverage: number;
  totalMemoryBytes: number;
  usedMemoryBytes: number;
  totalDiskBytes: number;
  usedDiskBytes: number;
  uptimeSeconds: number;
}

export interface JudgeRuntimeQueueData {
  dbmsType: string;
  dbmsLabel: string;
  waitingCount: number;
}

export interface JudgeRuntimeNodeData {
  databaseId: string;
  databaseName: string;
  dbmsType: string;
  dbmsLabel: string;
  runnerContainer: string;
  enabled: boolean;
  ready: boolean;
  configuredMaxConcurrency: number;
  effectiveMaxConcurrency: number;
  runningCount: number;
  availableRunnerCount: number;
  totalPortCount: number;
  availablePortCount: number;
}

export interface DockerContainerData {
  name: string;
  status: string;
  cpuPercent: string;
  memoryUsage: string;
}

export interface JudgeConfigData {
  databaseId: string;
  databaseName: string;
  dbmsType: string;
  dbmsLabel: string;
  enabled: boolean;
  maxConcurrency: number;
  updatedAt: string;
}

export interface DbRuntimeData {
  totalWaitingCount: number;
  totalRunningCount: number;
  queues: JudgeRuntimeQueueData[];
  nodes: JudgeRuntimeNodeData[];
  containers: DockerContainerData[];
  configs: JudgeConfigData[];
}

export interface ServerLogData {
  level: string;
  date: string;
  exists: boolean;
  lines: string[];
}

export interface JudgeConfigUpdatePayload {
  enabled: boolean;
  maxConcurrency: number;
}

function normalizeSystemResource(data: SystemResourceResponse): SystemResourceData {
  return {
    systemCpuUsagePercent: data.systemCpuUsagePercent ?? 0,
    processCpuUsagePercent: data.processCpuUsagePercent ?? 0,
    systemLoadAverage: data.systemLoadAverage ?? 0,
    totalMemoryBytes: data.totalMemoryBytes ?? 0,
    usedMemoryBytes: data.usedMemoryBytes ?? 0,
    totalDiskBytes: data.totalDiskBytes ?? 0,
    usedDiskBytes: data.usedDiskBytes ?? 0,
    uptimeSeconds: data.uptimeSeconds ?? 0,
  };
}

function normalizeDbRuntime(data: DbRuntimeResponse): DbRuntimeData {
  return {
    totalWaitingCount: data.totalWaitingCount ?? 0,
    totalRunningCount: data.totalRunningCount ?? 0,
    queues: Array.isArray(data.queues)
      ? data.queues.map((queue) => ({
          dbmsType: queue.dbmsType ?? '',
          dbmsLabel: queue.dbmsLabel ?? '',
          waitingCount: queue.waitingCount ?? 0,
        }))
      : [],
    nodes: Array.isArray(data.nodes)
      ? data.nodes.map((node) => ({
          databaseId: node.databaseId ?? '',
          databaseName: node.databaseName ?? '',
          dbmsType: node.dbmsType ?? '',
          dbmsLabel: node.dbmsLabel ?? '',
          runnerContainer: node.runnerContainer ?? '',
          enabled: Boolean(node.enabled),
          ready: Boolean(node.ready),
          configuredMaxConcurrency: node.configuredMaxConcurrency ?? 0,
          effectiveMaxConcurrency: node.effectiveMaxConcurrency ?? 0,
          runningCount: node.runningCount ?? 0,
          availableRunnerCount: node.availableRunnerCount ?? 0,
          totalPortCount: node.totalPortCount ?? 0,
          availablePortCount: node.availablePortCount ?? 0,
        }))
      : [],
    containers: Array.isArray(data.containers)
      ? data.containers.map((container) => ({
          name: container.name ?? '',
          status: container.status ?? 'unknown',
          cpuPercent: container.cpuPercent ?? '-',
          memoryUsage: container.memoryUsage ?? '-',
        }))
      : [],
    configs: Array.isArray(data.configs)
      ? data.configs.map(normalizeJudgeConfig)
      : [],
  };
}

export function normalizeSystemResourceData(data: unknown): SystemResourceData {
  return normalizeSystemResource((data ?? {}) as SystemResourceResponse);
}

export function normalizeDbRuntimeData(data: unknown): DbRuntimeData {
  return normalizeDbRuntime((data ?? {}) as DbRuntimeResponse);
}

export function normalizeServerLogData(data: unknown): ServerLogData {
  return normalizeServerLog((data ?? {}) as ServerLogResponse);
}

function normalizeJudgeConfig(data: JudgeConfigResponse): JudgeConfigData {
  return {
    databaseId: data.databaseId ?? '',
    databaseName: data.databaseName ?? '',
    dbmsType: data.dbmsType ?? '',
    dbmsLabel: data.dbmsLabel ?? '',
    enabled: Boolean(data.enabled),
    maxConcurrency: data.maxConcurrency ?? 1,
    updatedAt: data.updatedAt ?? '',
  };
}

function normalizeServerLog(data: ServerLogResponse): ServerLogData {
  return {
    level: data.level ?? 'info',
    date: data.date ?? '',
    exists: Boolean(data.exists),
    lines: Array.isArray(data.lines) ? data.lines : [],
  };
}

async function requestMonitoring<T>(path: string, init: RequestInit, fallbackMessage: string, normalize: (data: unknown) => T): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      credentials: 'include',
      ...init,
    });
  } catch {
    throw new Error(fallbackMessage);
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, fallbackMessage);
  }

  try {
    return normalize(await response.json());
  } catch {
    throw new Error(fallbackMessage);
  }
}

export function fetchSystemResources(): Promise<SystemResourceData> {
  return requestMonitoring(
    '/admin/monitoring/resources',
    { method: 'GET' },
    getUiTextValue('MONITORING_RESOURCE_LOAD_FAIL_MESSAGE', '서버 리소스를 불러오지 못했습니다.'),
    normalizeSystemResourceData,
  );
}

export function fetchDbRuntime(): Promise<DbRuntimeData> {
  return requestMonitoring(
    '/admin/monitoring/db-runtime',
    { method: 'GET' },
    getUiTextValue('MONITORING_RUNTIME_LOAD_FAIL_MESSAGE', 'DB Runtime 상태를 불러오지 못했습니다.'),
    normalizeDbRuntimeData,
  );
}

export function fetchServerLogs(level: string, date: string, size = 500): Promise<ServerLogData> {
  const params = new URLSearchParams({ level, date, size: String(size) });
  return requestMonitoring(
    `/admin/monitoring/logs?${params.toString()}`,
    { method: 'GET' },
    getUiTextValue('MONITORING_LOG_LOAD_FAIL_MESSAGE', '서버 로그를 불러오지 못했습니다.'),
    (data) => normalizeServerLog((data ?? {}) as ServerLogResponse),
  );
}

export function updateJudgeConfig(databaseId: string, payload: JudgeConfigUpdatePayload): Promise<JudgeConfigData> {
  return requestMonitoring(
    `/admin/monitoring/judge-configs/${encodeURIComponent(databaseId)}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
    getUiTextValue('MONITORING_CONFIG_SAVE_FAIL_MESSAGE', 'judge 설정을 저장하지 못했습니다.'),
    (data) => normalizeJudgeConfig((data ?? {}) as JudgeConfigResponse),
  );
}
