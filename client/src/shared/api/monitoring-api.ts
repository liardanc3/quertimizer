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

interface DatabaseQueueResponse {
  dbmsType?: string;
  dbmsLabel?: string;
  waitingCount?: number;
}

interface DatabaseNodeResponse {
  databaseId?: string;
  databaseName?: string;
  dbmsType?: string;
  dbmsLabel?: string;
  containerName?: string;
  enabled?: boolean;
  ready?: boolean;
  configuredMaxConcurrency?: number;
  effectiveMaxConcurrency?: number;
  runningCount?: number;
  availableDatabaseCount?: number;
  totalPortCount?: number;
  availablePortCount?: number;
}

interface DockerContainerResponse {
  name?: string;
  status?: string;
  cpuPercent?: string;
  memoryUsage?: string;
}

interface DatabaseNodeConfigResponse {
  databaseId?: string;
  databaseName?: string;
  dbmsType?: string;
  dbmsLabel?: string;
  enabled?: boolean;
  maxConcurrency?: number;
  updatedAt?: string;
}

interface DatabaseStatusResponse {
  totalWaitingCount?: number;
  totalRunningCount?: number;
  queues?: DatabaseQueueResponse[];
  nodes?: DatabaseNodeResponse[];
  containers?: DockerContainerResponse[];
  configs?: DatabaseNodeConfigResponse[];
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

export interface DatabaseQueueData {
  dbmsType: string;
  dbmsLabel: string;
  waitingCount: number;
}

export interface DatabaseNodeData {
  databaseId: string;
  databaseName: string;
  dbmsType: string;
  dbmsLabel: string;
  containerName: string;
  enabled: boolean;
  ready: boolean;
  configuredMaxConcurrency: number;
  effectiveMaxConcurrency: number;
  runningCount: number;
  availableDatabaseCount: number;
  totalPortCount: number;
  availablePortCount: number;
}

export interface DockerContainerData {
  name: string;
  status: string;
  cpuPercent: string;
  memoryUsage: string;
}

export interface DatabaseNodeConfigData {
  databaseId: string;
  databaseName: string;
  dbmsType: string;
  dbmsLabel: string;
  enabled: boolean;
  maxConcurrency: number;
  updatedAt: string;
}

export interface DatabaseStatusData {
  totalWaitingCount: number;
  totalRunningCount: number;
  queues: DatabaseQueueData[];
  nodes: DatabaseNodeData[];
  containers: DockerContainerData[];
  configs: DatabaseNodeConfigData[];
}

export interface ServerLogData {
  level: string;
  date: string;
  exists: boolean;
  lines: string[];
}

export interface DatabaseNodeConfigUpdatePayload {
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

function normalizeDatabaseStatus(data: DatabaseStatusResponse): DatabaseStatusData {
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
          containerName: node.containerName ?? '',
          enabled: Boolean(node.enabled),
          ready: Boolean(node.ready),
          configuredMaxConcurrency: node.configuredMaxConcurrency ?? 0,
          effectiveMaxConcurrency: node.effectiveMaxConcurrency ?? 0,
          runningCount: node.runningCount ?? 0,
          availableDatabaseCount: node.availableDatabaseCount ?? 0,
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
      ? data.configs.map(normalizeDatabaseNodeConfig)
      : [],
  };
}

export function normalizeSystemResourceData(data: unknown): SystemResourceData {
  return normalizeSystemResource((data ?? {}) as SystemResourceResponse);
}

export function normalizeDatabaseStatusData(data: unknown): DatabaseStatusData {
  return normalizeDatabaseStatus((data ?? {}) as DatabaseStatusResponse);
}

export function normalizeServerLogData(data: unknown): ServerLogData {
  return normalizeServerLog((data ?? {}) as ServerLogResponse);
}

function normalizeDatabaseNodeConfig(data: DatabaseNodeConfigResponse): DatabaseNodeConfigData {
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

export function updateDatabaseNodeConfig(databaseId: string, payload: DatabaseNodeConfigUpdatePayload): Promise<DatabaseNodeConfigData> {
  return requestMonitoring(
    `/admin/monitoring/database-node-configs/${encodeURIComponent(databaseId)}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
    getUiTextValue('MONITORING_CONFIG_SAVE_FAIL_MESSAGE', 'DB 노드 설정을 저장하지 못했습니다.'),
    (data) => normalizeDatabaseNodeConfig((data ?? {}) as DatabaseNodeConfigResponse),
  );
}
