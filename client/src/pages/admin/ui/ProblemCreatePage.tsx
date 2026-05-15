import { useEffect, useMemo, useRef, useState, type Dispatch, type ReactNode, type SetStateAction } from 'react';
import { StatusPopup } from '@/shared/ui';
import useDismissableLayer from '@/shared/lib/hooks/use-dismissable-layer';
import {
  connectSessionSocket,
  subscribeSessionSocketConnection,
  subscribeSessionSocketMessages,
  type SessionSocketMessage,
} from '@/shared/auth/session-socket';
import {
  createProblem,
  fetchAdminProblemOptions,
  fetchProblemDetail,
  fetchProblemSetDetail,
  fetchProblemSets,
  previewProblemExamples,
  type DbmsType,
  type ProblemDataExampleTableData,
  type ProblemDataPreviewData,
  type ProblemDetailData,
  type ProblemOutputPreviewData,
  type ProblemSetDetailData,
  type ProblemSetSummary,
} from '@/shared/api/problem-api';
import { navigate } from '@/shared/config/navigation';
import { getUiText, getUiTextValue, useUiText } from '@/shared/config/ui-text';
import { ReactFlowDiagram } from '@/shared/ui/react-flow-diagram';
import './ProblemCreatePage.css';

type SectionKey = 'condition' | 'output' | 'ddl' | 'actualData' | 'hiddenData' | 'dataPreview' | 'outputPreview' | 'answerSql';
type MissingFieldKey = 'title' | 'description' | 'condition' | 'output' | 'ddl' | 'actualData' | 'hiddenData' | 'answerSql';

interface EditableDraftState<T> {
  appliedValue: T;
  draftValue: T;
  isEditing: boolean;
  setDraftValue: Dispatch<SetStateAction<T>>;
  startEditing: () => void;
  cancelEditing: () => void;
  confirmEditing: () => void;
  replaceValue: (nextValue: T) => void;
}

interface SourceDraft {
  postgresql: string;
  mysql: string;
}

interface PopupState {
  open: boolean;
  level: 1 | 2 | 3;
  message: string;
}

interface ProblemCreateSelectOption {
  value: string;
  label: string;
}

interface MissingField {
  key: MissingFieldKey;
  label: string;
}

interface ProblemCreateProgressStep {
  stepKey: string;
  status: 'waiting' | 'running' | 'success' | 'error';
  message: string;
  stepOrder: number;
}

interface ProblemCreateProgressMessage extends SessionSocketMessage {
  type: 'problem.create.progress';
  stepKey?: unknown;
  status?: unknown;
  message?: unknown;
  stepOrder?: unknown;
  problemId?: unknown;
}

interface SectionRefs {
  condition: HTMLElement | null;
  output: HTMLElement | null;
  ddl: HTMLElement | null;
  actualData: HTMLElement | null;
  hiddenData: HTMLElement | null;
  answerSql: HTMLElement | null;
}

interface ParsedSchemaColumn {
  name: string;
  type: string;
  description: string;
  primaryKey: boolean;
  foreignKey: boolean;
  reference?: {
    tableName: string;
    columnName: string;
  };
}

interface ParsedSchemaRelation {
  sourceTableName: string;
  sourceColumnName: string;
  targetTableName: string;
  targetColumnName: string;
}

interface ParsedSchemaTable {
  name: string;
  description: string;
  columns: ParsedSchemaColumn[];
}

interface ParsedSchemaDdl {
  tables: ParsedSchemaTable[];
  relations: ParsedSchemaRelation[];
}

const EMPTY_PROBLEM_SET_DETAIL: ProblemSetDetailData = {
  problemSetId: '',
  ddl: '',
  actualDataSql: '',
};

const EMPTY_PREVIEW_OUTPUT: ProblemOutputPreviewData = {
  columns: [],
  rows: [],
  rowCount: 0,
  visibleRows: 0,
  rowLimit: 10,
};

const EMPTY_DATA_PREVIEW: ProblemDataPreviewData = {
  rowLimit: 10,
  tables: [],
};

const NEW_PROBLEM_SET_OPTION_VALUE = '__new__';
const NEW_PROBLEM_OPTION_VALUE = '__new_problem__';
const SCHEMA_TABLE_COLUMN_TEMPLATE = 'minmax(9.2rem, 2fr) minmax(13rem, 3.4fr) minmax(8rem, 1.6fr) minmax(4.5rem, 1fr) minmax(9.2rem, 2fr)';
const ROW_COUNT_FORMATTER = new Intl.NumberFormat('ko-KR');
const MAX_HIDDEN_DATA_SQL_COUNT = 10;
const PROBLEM_CREATE_PROGRESS_TIMEOUT_MS = 10 * 60 * 1000;

function useEditableDraft<T>(initialValue: T): EditableDraftState<T> {
  const [appliedValue, setAppliedValue] = useState(initialValue);
  const [draftValue, setDraftValue] = useState(initialValue);
  const [isEditing, setIsEditing] = useState(false);

  function startEditing() {
    setDraftValue(appliedValue);
    setIsEditing(true);
  }

  function cancelEditing() {
    setDraftValue(appliedValue);
    setIsEditing(false);
  }

  function confirmEditing() {
    setAppliedValue(draftValue);
    setIsEditing(false);
  }

  function replaceValue(nextValue: T) {
    setAppliedValue(nextValue);
    setDraftValue(nextValue);
    setIsEditing(false);
  }

  return {
    appliedValue,
    draftValue,
    isEditing,
    setDraftValue,
    startEditing,
    cancelEditing,
    confirmEditing,
    replaceValue,
  };
}

function getProblemSetLabel(problemSetId: string) {
  return getUiText('PROBLEM_CREATE_SET_LABEL', { problemSetId }, `테이블셋 ${problemSetId}`);
}

function getProblemLabel(problemId: string) {
  return getUiText('PROBLEM_CREATE_PROBLEM_LABEL', { problemId }, `문제 ${problemId}`);
}

function getProblemNumberLabel(existingProblemSet: boolean, existingProblem: boolean, problemSetId: string | null, problemId: string | null) {
  if (existingProblemSet && existingProblem && problemId) {
    return problemId;
  }

  if (existingProblemSet && problemSetId) {
    return getUiText('PROBLEM_CREATE_NEW_SET_NUMBER_LABEL', { problemSetId }, `${problemSetId}-신규`);
  }

  return getUiTextValue('PROBLEM_CREATE_NEW_PROBLEM_LABEL', '신규 문제');
}

function resolveScopedDbms(value: string | null | undefined): DbmsType {
  return value?.trim().startsWith('M') ? 'mysql' : 'postgresql';
}

function getTableNamesFromDdl(ddl: string) {
  const pattern = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\s*\(/gi;
  const tableNames: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(ddl)) != null) {
    if (!tableNames.includes(match[1])) {
      tableNames.push(match[1]);
    }
  }

  return tableNames;
}

function filterDdlByTableNames(ddl: string, includedTableNames: string[]) {
  if (includedTableNames.length === 0 || ddl.trim() === '') {
    return ddl.trim();
  }

  const tableNameSet = new Set(includedTableNames);
  const createTablePattern = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\s*\([\s\S]*?\)\s*(?:ENGINE\s*=\s*\w+\s*)?(?:(?:DEFAULT\s+)?CHARSET\s*=\s*[\w\d_]+\s*)?(?:COLLATE\s*=\s*[\w\d_]+\s*)?(?:COMMENT\s*=\s*'[^']*'\s*)?;/gi;
  const commentPattern = /COMMENT ON (TABLE|COLUMN)\s+([\s\S]*?);/gi;
  const fragments: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    if (tableNameSet.has(match[1])) {
      fragments.push(match[0].trim());
    }
  }

  while ((match = commentPattern.exec(ddl)) != null) {
    const targetTableName = resolveCommentTargetTableName(match[1], match[2]);
    if (targetTableName && tableNameSet.has(targetTableName)) {
      fragments.push(`COMMENT ON ${match[1]} ${match[2]};`.trim());
    }
  }

  return fragments.join('\n\n').trim();
}

function resolveCommentTargetTableName(commentKind: string, commentBody: string) {
  const targetExpression = commentBody.match(/^([\s\S]*?)\s+IS\s+/i)?.[1].trim() ?? '';
  const targetTokens = targetExpression.replace(/[`"]/g, '').split('.').map((token) => token.trim()).filter(Boolean);

  if (commentKind.toUpperCase() === 'TABLE') {
    return targetTokens[targetTokens.length - 1] ?? '';
  }

  return targetTokens[targetTokens.length - 2] ?? '';
}

function parseSchemaMetadata(rawSchemaMetadata: string): ParsedSchemaDdl {
  if (rawSchemaMetadata.trim() === '') {
    return { tables: [], relations: [] };
  }

  try {
    const parsed = JSON.parse(rawSchemaMetadata) as { tables?: unknown; relations?: unknown };
    const tables = Array.isArray(parsed.tables)
      ? parsed.tables
          .filter((table): table is Record<string, unknown> => typeof table === 'object' && table != null)
          .map((table) => ({
            name: typeof table.name === 'string' ? table.name : '',
            description: typeof table.description === 'string' ? table.description : '',
            columns: Array.isArray(table.columns)
              ? table.columns
                  .filter((column): column is Record<string, unknown> => typeof column === 'object' && column != null)
                  .map((column) => {
                    const reference = typeof column.reference === 'object' && column.reference != null
                      ? column.reference as Record<string, unknown>
                      : null;

                    return {
                      name: typeof column.name === 'string' ? column.name : '',
                      type: typeof column.type === 'string' ? column.type : '',
                      description: typeof column.description === 'string' ? column.description : '',
                      primaryKey: column.primaryKey === true,
                      foreignKey: column.foreignKey === true,
                      reference: reference == null ? undefined : {
                        tableName: typeof reference.tableName === 'string' ? reference.tableName : '',
                        columnName: typeof reference.columnName === 'string' ? reference.columnName : '',
                      },
                    };
                  })
                  .filter((column) => column.name !== '')
              : [],
          }))
          .filter((table) => table.name !== '')
      : [];
    const relations = Array.isArray(parsed.relations)
      ? parsed.relations
          .filter((relation): relation is Record<string, unknown> => typeof relation === 'object' && relation != null)
          .map((relation) => ({
            sourceTableName: typeof relation.sourceTableName === 'string' ? relation.sourceTableName : '',
            sourceColumnName: typeof relation.sourceColumnName === 'string' ? relation.sourceColumnName : '',
            targetTableName: typeof relation.targetTableName === 'string' ? relation.targetTableName : '',
            targetColumnName: typeof relation.targetColumnName === 'string' ? relation.targetColumnName : '',
          }))
          .filter((relation) => relation.sourceTableName !== '' && relation.targetTableName !== '')
      : [];

    return { tables, relations: dedupeRelations(relations) };
  } catch {
    return { tables: [], relations: [] };
  }
}

function parseSchemaDdl(ddl: string): ParsedSchemaDdl {
  const createTablePattern = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\s*\(([\s\S]*?)\)\s*(?:ENGINE\s*=\s*\w+\s*)?(?:(?:DEFAULT\s+)?CHARSET\s*=\s*[\w\d_]+\s*)?(?:COLLATE\s*=\s*[\w\d_]+\s*)?(?:COMMENT\s*=\s*'((?:''|[^'])*)'\s*)?;/gi;
  const tableComments = parseTableComments(ddl);
  const columnComments = parseColumnComments(ddl);
  const tables: ParsedSchemaTable[] = [];
  const relations: ParsedSchemaRelation[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    const tableName = normalizeSqlIdentifier(match[1]);
    const columns: ParsedSchemaColumn[] = [];

    splitCreateTableItems(match[2]).forEach((line) => {
      if (/^CONSTRAINT\s+/i.test(line) || /^FOREIGN\s+KEY\s*\(/i.test(line)) {
        const foreignKeyMatch = line.match(/FOREIGN\s+KEY\s*\([`"]?(\w+)[`"]?\)\s+REFERENCES\s+(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\s*\([`"]?(\w+)[`"]?\)/i);

        if (!foreignKeyMatch) {
          return;
        }

        const sourceColumnName = normalizeSqlIdentifier(foreignKeyMatch[1]);
        const targetTableName = normalizeSqlIdentifier(foreignKeyMatch[2]);
        const targetColumnName = normalizeSqlIdentifier(foreignKeyMatch[3]);
        const sourceColumn = columns.find((column) => column.name === sourceColumnName);

        if (sourceColumn) {
          sourceColumn.foreignKey = true;
          sourceColumn.reference = { tableName: targetTableName, columnName: targetColumnName };
        }

        relations.push({
          sourceTableName: targetTableName, sourceColumnName: targetColumnName,
          targetTableName: tableName, targetColumnName: sourceColumnName,
        });
        return;
      }

      if (/^PRIMARY\s+KEY\s*\(/i.test(line)) {
        extractKeyColumnNames(line).forEach((primaryKeyColumnName) => {
          const targetColumn = columns.find((column) => column.name === primaryKeyColumnName);
          if (targetColumn) {
            targetColumn.primaryKey = true;
          }
        });
        return;
      }

      const columnMatch = line.match(/^[`"]?(\w+)[`"]?\s+(.+)$/);
      if (!columnMatch) {
        return;
      }

      const columnName = normalizeSqlIdentifier(columnMatch[1]);
      const remainder = columnMatch[2];
      const referenceMatch = remainder.match(/REFERENCES\s+(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\s*\([`"]?(\w+)[`"]?\)/i);
      const columnDescription = columnComments.get(`${tableName}.${columnName}`) ?? parseInlineColumnComment(remainder) ?? describeSchemaColumn(columnName);

      columns.push({
        name: columnName,
        type: extractColumnType(remainder),
        description: columnDescription,
        primaryKey: /PRIMARY\s+KEY/i.test(remainder),
        foreignKey: referenceMatch != null,
        reference: referenceMatch == null
          ? undefined
          : { tableName: normalizeSqlIdentifier(referenceMatch[1]), columnName: normalizeSqlIdentifier(referenceMatch[2]) },
      });

      if (referenceMatch != null) {
        relations.push({
          sourceTableName: normalizeSqlIdentifier(referenceMatch[1]), sourceColumnName: normalizeSqlIdentifier(referenceMatch[2]),
          targetTableName: tableName, targetColumnName: columnName,
        });
      }
    });

    tables.push({
      name: tableName,
      description: tableComments.get(tableName) ?? (match[3] ? decodeSqlComment(match[3]) : `${formatSchemaIdentifier(tableName)} 테이블`),
      columns,
    });
  }

  return { tables, relations: dedupeRelations(relations) };
}

function splitCreateTableItems(definition: string) {
  const items: string[] = [];
  let startIndex = 0;
  let depth = 0;
  let quote: string | null = null;

  for (let index = 0; index < definition.length; index += 1) {
    const currentCharacter = definition[index];
    const nextCharacter = definition[index + 1];

    if (quote != null) {
      if (currentCharacter === quote) {
        if (quote === "'" && nextCharacter === "'") {
          index += 1;
          continue;
        }

        quote = null;
      }
      continue;
    }

    if (currentCharacter === "'" || currentCharacter === '"' || currentCharacter === '`') {
      quote = currentCharacter;
      continue;
    }

    if (currentCharacter === '(') {
      depth += 1;
      continue;
    }

    if (currentCharacter === ')') {
      depth = Math.max(depth - 1, 0);
      continue;
    }

    if (currentCharacter === ',' && depth === 0) {
      items.push(definition.slice(startIndex, index).trim());
      startIndex = index + 1;
    }
  }

  items.push(definition.slice(startIndex).trim());
  return items.map((item) => item.replace(/,$/, '').trim()).filter(Boolean);
}

function parseTableComments(ddl: string) {
  const tableCommentPattern = /COMMENT\s+ON\s+TABLE\s+(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\s+IS\s+'((?:''|[^'])*)';/gi;
  const tableComments = new Map<string, string>();
  let match: RegExpExecArray | null;

  while ((match = tableCommentPattern.exec(ddl)) != null) {
    tableComments.set(normalizeSqlIdentifier(match[1]), decodeSqlComment(match[2]));
  }

  return tableComments;
}

function parseColumnComments(ddl: string) {
  const columnCommentPattern = /COMMENT\s+ON\s+COLUMN\s+(?:[`"\w]+\.)?[`"]?(\w+)[`"]?\.[`"]?(\w+)[`"]?\s+IS\s+'((?:''|[^'])*)';/gi;
  const columnComments = new Map<string, string>();
  let match: RegExpExecArray | null;

  while ((match = columnCommentPattern.exec(ddl)) != null) {
    columnComments.set(`${normalizeSqlIdentifier(match[1])}.${normalizeSqlIdentifier(match[2])}`, decodeSqlComment(match[3]));
  }

  return columnComments;
}

function parseInlineColumnComment(remainder: string) {
  const commentMatch = remainder.match(/\s+COMMENT\s+'((?:''|[^'])*)'/i);
  return commentMatch ? decodeSqlComment(commentMatch[1]) : null;
}

function decodeSqlComment(value: string) {
  return value.replace(/''/g, "'");
}

function extractColumnType(remainder: string) {
  return remainder
    .replace(/\s+NOT\s+NULL/gi, '')
    .replace(/\s+NULL/gi, '')
    .replace(/\s+PRIMARY\s+KEY/gi, '')
    .replace(/\s+AUTO_INCREMENT/gi, '')
    .replace(/\s+CHECK\s*\(.+$/gi, '')
    .replace(/\s+DEFAULT\s+.+$/gi, '')
    .replace(/\s+REFERENCES\s+.+$/gi, '')
    .replace(/\s+COMMENT\s+'.+$/gi, '')
    .trim();
}

function extractKeyColumnNames(line: string) {
  return (line.match(/\(([^)]+)\)/)?.[1] ?? '')
    .split(',')
    .map((columnName) => normalizeSqlIdentifier(columnName))
    .filter(Boolean);
}

function dedupeRelations(relations: ParsedSchemaRelation[]) {
  const relationMap = new Map<string, ParsedSchemaRelation>();

  relations.forEach((relation) => {
    relationMap.set([
      relation.sourceTableName, relation.sourceColumnName,
      relation.targetTableName, relation.targetColumnName,
    ].join(':'), relation);
  });

  return Array.from(relationMap.values());
}

function normalizeSqlIdentifier(value: string) {
  return value.trim().replace(/^[`"]/, '').replace(/[`"]$/, '');
}

function describeSchemaColumn(columnName: string) {
  if (columnName.endsWith('_id')) {
    return `${formatSchemaIdentifier(columnName.replace(/_id$/, ''))} ID`;
  }

  if (columnName.endsWith('_at')) {
    return `${formatSchemaIdentifier(columnName.replace(/_at$/, ''))} 시각`;
  }

  if (columnName.endsWith('_date')) {
    return `${formatSchemaIdentifier(columnName.replace(/_date$/, ''))} 날짜`;
  }

  if (columnName.endsWith('_amount')) {
    return `${formatSchemaIdentifier(columnName.replace(/_amount$/, ''))} 금액`;
  }

  return formatSchemaIdentifier(columnName);
}

function formatSchemaIdentifier(value: string) {
  return value
    .split('_')
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ');
}

function formatColumnKey(column: ParsedSchemaColumn) {
  if (column.primaryKey && column.foreignKey) return 'PK, FK';
  if (column.primaryKey) return 'PK';
  if (column.foreignKey) return 'FK';
  return '-';
}

function renderSchemaGridCell(value: string, bodyClassName?: string) {
  return (
    <div className={bodyClassName ? `solve-detail-grid-cell ${bodyClassName}` : 'solve-detail-grid-cell'}>
      {value}
    </div>
  );
}

function arraysEqual(left: string[], right: string[]) {
  if (left.length !== right.length) {
    return false;
  }

  return left.every((value, index) => value === right[index]);
}

function parseCsvLine(line: string) {
  const cells: string[] = [];
  let currentValue = '';
  let inQuote = false;

  for (let index = 0; index < line.length; index += 1) {
    const currentCharacter = line[index];
    const nextCharacter = line[index + 1];

    if (currentCharacter === '"') {
      if (inQuote && nextCharacter === '"') {
        currentValue += '"';
        index += 1;
        continue;
      }

      inQuote = !inQuote;
      continue;
    }

    if (!inQuote && currentCharacter === ',') {
      cells.push(currentValue.trim());
      currentValue = '';
      continue;
    }

    currentValue += currentCharacter;
  }

  cells.push(currentValue.trim());
  return cells;
}

function parseCsvValue(value: string): string | number | boolean | null {
  if (/^null$/i.test(value)) return null;
  if (/^true$/i.test(value)) return true;
  if (/^false$/i.test(value)) return false;
  if (/^-?\d+(?:\.\d+)?$/.test(value)) return Number(value);
  return value;
}

function formatExampleSummary(totalRows: number, visibleRows: number) {
  return `… 총 ${ROW_COUNT_FORMATTER.format(totalRows)}행 중 ${ROW_COUNT_FORMATTER.format(visibleRows)}행 표시`;
}

function normalizeExampleRows(rows: unknown): Array<Array<string | number | boolean | null>> {
  if (!Array.isArray(rows)) {
    return [];
  }

  return rows.map((row) =>
    Array.isArray(row)
      ? row.map((value) =>
          typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || value == null
            ? value
            : String(value),
        )
      : [],
  );
}

function parseProblemDataExample(rawDataExample: string): ProblemDataExampleTableData[] {
  if (rawDataExample.trim() === '') {
    return [];
  }

  try {
    const parsed = JSON.parse(rawDataExample) as { tables?: unknown };
    if (!Array.isArray(parsed.tables)) {
      return [];
    }

    return parsed.tables
      .filter((table): table is { name: unknown; columns: unknown; rows: unknown; totalRows?: unknown; visibleRows?: unknown } =>
        typeof table === 'object' && table != null,
      )
      .map((table) => {
        const rows = normalizeExampleRows(table.rows);
        return {
          name: typeof table.name === 'string' ? table.name : '',
          columns: Array.isArray(table.columns) ? table.columns.filter((column): column is string => typeof column === 'string') : [],
          rows,
          totalRows: typeof table.totalRows === 'number' ? table.totalRows : rows.length,
          visibleRows: typeof table.visibleRows === 'number' ? table.visibleRows : rows.length,
        };
      })
      .filter((table) => table.name !== '' && table.columns.length > 0);
  } catch {
    return [];
  }
}

function parseProblemOutputExample(rawOutputExample: string): ProblemOutputPreviewData {
  if (rawOutputExample.trim() === '') {
    return EMPTY_PREVIEW_OUTPUT;
  }

  try {
    const parsed = JSON.parse(rawOutputExample) as {
      columns?: unknown;
      rows?: unknown;
      totalRows?: unknown;
      visibleRows?: unknown;
      rowLimit?: unknown;
    };
    if (Array.isArray(parsed.columns) && Array.isArray(parsed.rows)) {
      const rows = normalizeExampleRows(parsed.rows);

      return {
        columns: parsed.columns.filter((column): column is string => typeof column === 'string'),
        rows,
        rowCount: typeof parsed.totalRows === 'number' ? parsed.totalRows : rows.length,
        visibleRows: typeof parsed.visibleRows === 'number' ? parsed.visibleRows : rows.length,
        rowLimit: typeof parsed.rowLimit === 'number' ? parsed.rowLimit : 10,
      };
    }
  } catch {
  }

  try {
    const lines = rawOutputExample
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);

    if (lines.length === 0) {
      return EMPTY_PREVIEW_OUTPUT;
    }

    const [headerLine, ...dataLines] = lines;
    const rows = dataLines.map((line) => parseCsvLine(line).map(parseCsvValue));

    return {
      columns: parseCsvLine(headerLine),
      rows,
      rowCount: rows.length,
      visibleRows: rows.length,
      rowLimit: 10,
    };
  } catch {
    return EMPTY_PREVIEW_OUTPUT;
  }
}

function buildMissingFields(values: {
  title: string;
  description: string;
  condition: string;
  output: string;
  ddl: string;
  actualData: string;
  hiddenDataSqls: string[];
  requireHiddenData: boolean;
  answerSql: string;
}) {
  const missingFields: MissingField[] = [];

  if (values.title.trim() === '') missingFields.push({ key: 'title', label: getUiTextValue('PROBLEM_CREATE_TITLE_LABEL', '문제 제목') });
  if (values.description.trim() === '') missingFields.push({ key: 'description', label: getUiTextValue('PROBLEM_CREATE_DESCRIPTION_LABEL', '설명') });
  if (values.condition.trim() === '') missingFields.push({ key: 'condition', label: getUiTextValue('PROBLEM_CREATE_CONDITION_LABEL', '조건') });
  if (values.output.trim() === '') missingFields.push({ key: 'output', label: getUiTextValue('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명') });
  if (values.ddl.trim() === '') missingFields.push({ key: 'ddl', label: getUiTextValue('PROBLEM_CREATE_DDL_LABEL', '테이블 정보 DDL') });
  if (values.actualData.trim() === '') missingFields.push({ key: 'actualData', label: getUiTextValue('PROBLEM_CREATE_ACTUAL_DATA_LABEL', '실제 채점 데이터') });
  if (values.requireHiddenData && values.hiddenDataSqls.every((hiddenDataSql) => hiddenDataSql.trim() === '')) {
    missingFields.push({ key: 'hiddenData', label: getUiTextValue('PROBLEM_CREATE_HIDDEN_DATA_LABEL', '채점용 데이터 - Hidden') });
  }
  if (values.answerSql.trim() === '') missingFields.push({ key: 'answerSql', label: getUiTextValue('PROBLEM_CREATE_ANSWER_SQL_LABEL', '정답 SQL') });

  return missingFields;
}

function isProblemCreateProgressMessage(message: SessionSocketMessage): message is ProblemCreateProgressMessage {
  return message.type === 'problem.create.progress';
}

function upsertCreateProgressStep(currentSteps: ProblemCreateProgressStep[], nextStep: ProblemCreateProgressStep) {
  const completedSteps = nextStep.status === 'waiting'
    ? currentSteps
    : currentSteps.map((step) => {
      if (step.stepOrder >= nextStep.stepOrder || step.status === 'success' || step.status === 'error') {
        return step;
      }
      return { ...step, status: 'success' as const, message: createCreateProgressSuccessMessage(step.message) };
    });
  const nextSteps = completedSteps.filter((step) => step.stepKey !== nextStep.stepKey);
  return [...nextSteps, nextStep].sort((left, right) => left.stepOrder - right.stepOrder);
}

function createCreateProgressSuccessMessage(message: string) {
  if (message.endsWith(' 대기 중')) {
    return `${message.slice(0, -' 대기 중'.length)} 완료`;
  }
  if (message.endsWith(' 중')) {
    return `${message.slice(0, -' 중'.length)} 완료`;
  }
  return message;
}

function createInitialCreateProgressSteps(hiddenDataCount: number, existingProblemSet: boolean, existingProblem: boolean): ProblemCreateProgressStep[] {
  if (existingProblem) {
    return [{ stepKey: 'problem-text', status: 'running', message: '문제 정보 저장 중', stepOrder: 1 }];
  }

  return [
    ...(!existingProblemSet ? [{ stepKey: 'open-data', status: 'running' as const, message: '채점용 데이터 INSERT - Open 생성 중', stepOrder: 1 }] : []),
    { stepKey: 'answer-hash', status: existingProblemSet ? 'running' : 'waiting', message: `정답 해시 생성 ${existingProblemSet ? '중' : '대기 중'}`, stepOrder: 2 },
    { stepKey: 'table-info', status: 'waiting', message: '테이블 정보 생성 대기 중', stepOrder: 3 },
    { stepKey: 'erd-info', status: 'waiting', message: 'ERD 정보 생성 대기 중', stepOrder: 4 },
    { stepKey: 'data-example', status: 'waiting', message: '데이터 예시 생성 대기 중', stepOrder: 5 },
    { stepKey: 'output-example', status: 'waiting', message: '출력 예시 생성 대기 중', stepOrder: 6 },
    ...Array.from({ length: hiddenDataCount }, (_, index) => ({
      stepKey: `hidden-data-${index + 1}`,
      status: 'waiting' as const,
      message: `채점용 데이터 INSERT - Hidden ${index + 1} 생성 대기 중`,
      stepOrder: index + 7,
    })),
  ];
}

function EditIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="currentColor" d="M3 17.25V21h3.75l11-11.03-3.75-3.75L3 17.25Zm14.71-9.04a1.003 1.003 0 0 0 0-1.42l-2.5-2.5a1.003 1.003 0 0 0-1.42 0l-1.96 1.96 3.75 3.75 2.13-1.79Z" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="currentColor" d="M18.3 5.71 12 12l6.3 6.29-1.41 1.41L10.59 13.4 4.29 19.7 2.88 18.29 9.17 12 2.88 5.71 4.29 4.29l6.3 6.3 6.29-6.3 1.42 1.42Z" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="currentColor" d="m9 16.17-3.88-3.88L3.71 13.7 9 19l12-12-1.41-1.41z" />
    </svg>
  );
}

function SmallSpinner() {
  return <span className="problem-create-progress-spinner" aria-hidden="true" />;
}

function ProblemCreateProgressPopup({ steps }: { steps: ProblemCreateProgressStep[] }) {
  const visibleSteps = steps.length > 0
    ? steps
    : [{ stepKey: 'waiting', status: 'running' as const, message: '문제 저장 요청 처리 중', stepOrder: 0 }];

  return (
    <div className="problem-create-progress-scrim" role="dialog" aria-modal="true" aria-label="문제 생성 진행 상태">
      <div className="problem-create-progress-popup">
        <ol className="problem-create-progress-list">
          {visibleSteps.map((step) => (
            <li key={step.stepKey} className={`problem-create-progress-item is-${step.status}`}>
              <span className="problem-create-progress-icon">
                {step.status === 'success'
                  ? <CheckIcon />
                  : step.status === 'error'
                    ? <CloseIcon />
                    : step.status === 'waiting'
                      ? <span className="problem-create-progress-wait-dot" />
                      : <SmallSpinner />}
              </span>
              <span className="problem-create-progress-message">{step.message}</span>
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}

function ChevronIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d={collapsed ? 'm6 7.2 4 4 4-4' : 'm7.2 6 4 4-4 4'}
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function SelectChevronIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="m5.5 7.8 4.5 4.4 4.5-4.4" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  );
}

function InlineEditActions({ isEditing, onEdit, onCancel, onConfirm }: { isEditing: boolean; onEdit: () => void; onCancel: () => void; onConfirm: () => void }) {
  return (
    <div className="problem-create-inline-toolbar">
      <button
        type="button"
        className={`problem-create-inline-action ${isEditing ? 'is-active' : ''}`.trim()}
        aria-label={isEditing ? getUiTextValue('COMMON_CANCEL_BUTTON', '취소') : getUiTextValue('COMMON_EDIT_BUTTON', '수정')}
        onClick={isEditing ? onCancel : onEdit}
      >
        {isEditing ? <CloseIcon /> : <EditIcon />}
      </button>
      {isEditing ? (
        <button type="button" className="problem-create-inline-action problem-create-confirm-action" aria-label={getUiTextValue('COMMON_APPLY_BUTTON', '적용')} onClick={onConfirm}>
          <CheckIcon />
        </button>
      ) : null}
    </div>
  );
}

function ProblemCreateSelectMenu({
  value,
  options,
  onChange,
  className,
  disabled = false,
}: {
  value: string;
  options: ProblemCreateSelectOption[];
  onChange: (value: string) => void;
  className?: string;
  disabled?: boolean;
}) {
  const shellRef = useRef<HTMLDivElement | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const dismissLayerRefs = useMemo(() => [shellRef], []);
  const selectedOption = options.find((option) => option.value === value) ?? options[0];

  useDismissableLayer({
    enabled: isOpen,
    refs: dismissLayerRefs,
    onDismiss: () => setIsOpen(false),
  });

  return (
    <div ref={shellRef} className={`problem-create-select-shell ${isOpen ? 'is-open' : ''} ${disabled ? 'is-disabled' : ''}`.trim()}>
      <button
        type="button"
        className={`text-field problem-create-select-trigger ${className ?? ''}`.trim()}
        onClick={() => {
          if (!disabled) {
            setIsOpen((current) => !current);
          }
        }}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        disabled={disabled}
      >
        <span className="problem-create-select-trigger-text">{selectedOption?.label ?? ''}</span>
        <span className="problem-create-select-trigger-icon" aria-hidden="true">
          <SelectChevronIcon />
        </span>
      </button>

      {isOpen ? (
        <div className="problem-create-select-menu" role="listbox">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={option.value === value}
              className={`problem-create-select-option ${option.value === value ? 'is-selected' : ''}`.trim()}
              onClick={() => {
                onChange(option.value);
                setIsOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function ProblemCreateSection({
  sectionKey,
  title,
  collapsed,
  onToggle,
  isMissing,
  actions,
  children,
  sectionRef,
}: {
  sectionKey: SectionKey;
  title: string;
  collapsed: boolean;
  onToggle: (sectionKey: SectionKey) => void;
  isMissing?: boolean;
  actions?: ReactNode;
  children: ReactNode;
  sectionRef?: (node: HTMLElement | null) => void;
}) {
  return (
    <section ref={sectionRef} className={`problem-create-section ${collapsed ? 'is-collapsed' : ''} ${isMissing ? 'is-missing' : ''}`.trim()}>
      <div className="problem-create-section-header">
        <button
          type="button"
          className="problem-create-section-toggle"
          aria-label={collapsed ? getUiTextValue('COMMON_EXPAND_ACTION', '펼치기') : getUiTextValue('COMMON_COLLAPSE_ACTION', '접기')}
          aria-expanded={!collapsed}
          onClick={() => onToggle(sectionKey)}
        >
          <ChevronIcon collapsed={collapsed} />
        </button>
        <h2 className="problem-create-section-title">{title}</h2>
        <div className="problem-create-section-actions">{actions}</div>
      </div>

      {!collapsed ? <div className="problem-create-section-body">{children}</div> : null}
    </section>
  );
}

function ProblemCreateSchemaPreview({ ddl, schema }: { ddl: string; schema?: ParsedSchemaDdl }) {
  const { text } = useUiText();
  const parsedSchema = useMemo(() => schema ?? parseSchemaDdl(ddl), [ddl, schema]);
  const [collapsedSections, setCollapsedSections] = useState({ table: false, erd: false });
  const [selectedTableName, setSelectedTableName] = useState('');
  const [erdResetKey, setErdResetKey] = useState(0);
  const tableDefinitionColumns = useMemo(
    () => [
      { key: 'name', label: text('PROBLEM_DETAIL_COLUMN_NAME_LABEL', '컬럼명'), bodyClassName: 'solve-detail-grid-cell-name' },
      { key: 'description', label: text('PROBLEM_DETAIL_COLUMN_DESCRIPTION_LABEL', '설명') },
      { key: 'type', label: text('PROBLEM_DETAIL_COLUMN_TYPE_LABEL', '타입'), bodyClassName: 'solve-detail-grid-cell-type' },
      { key: 'key', label: text('PROBLEM_DETAIL_COLUMN_KEY_LABEL', '키') },
      { key: 'reference', label: text('PROBLEM_DETAIL_COLUMN_REFERENCE_LABEL', '참조') },
    ],
    [text],
  );
  const tableNamesSignature = parsedSchema.tables.map((table) => table.name).join('|');
  const selectedTable = parsedSchema.tables.find((table) => table.name === selectedTableName) ?? parsedSchema.tables[0];

  useEffect(() => {
    setSelectedTableName((current) =>
      parsedSchema.tables.some((table) => table.name === current) ? current : parsedSchema.tables[0]?.name ?? '',
    );
    setErdResetKey((current) => current + 1);
  }, [tableNamesSignature]);

  function toggleSchemaSection(sectionKey: 'table' | 'erd') {
    setCollapsedSections((current) => ({
      ...current,
      [sectionKey]: !current[sectionKey],
    }));
  }

  return (
    <div className="problem-create-schema-preview">
      <section className={`problem-create-section problem-create-schema-section ${collapsedSections.table ? 'is-collapsed' : ''}`.trim()}>
        <div className="problem-create-section-header">
          <button
            type="button"
            className="problem-create-section-toggle"
            aria-label={collapsedSections.table ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
            aria-expanded={!collapsedSections.table}
            onClick={() => toggleSchemaSection('table')}
          >
            <ChevronIcon collapsed={collapsedSections.table} />
          </button>
          <h2 className="problem-create-section-title">{text('PROBLEM_DETAIL_TABLE_SECTION_TITLE', '테이블 정보')}</h2>
          <div className="problem-create-section-actions" />
        </div>

        {!collapsedSections.table ? (
          <div className="problem-create-section-body">
            {parsedSchema.tables.length > 0 && selectedTable ? (
              <>
                <div className="solve-detail-table-tab-row">
                  {parsedSchema.tables.map((table) => (
                    <button
                      key={table.name}
                      type="button"
                      className={`solve-bookmark-button ${table.name === selectedTable.name ? 'is-selected' : ''}`.trim()}
                      aria-pressed={table.name === selectedTable.name}
                      onClick={() => setSelectedTableName(table.name)}
                    >
                      {table.name}
                    </button>
                  ))}
                </div>

                <div className="solve-detail-table-stack">
                  <div className="solve-detail-table-block">
                    <div className="solve-detail-table-block-header">
                      <div className="solve-detail-table-block-copy">
                        <p className="solve-detail-table-description">{selectedTable.description}</p>
                        <p className="solve-detail-table-name">{selectedTable.name}</p>
                      </div>
                    </div>

                    <div className="solve-detail-grid-table">
                      <div className="solve-detail-grid-row solve-detail-grid-row-head" style={{ gridTemplateColumns: SCHEMA_TABLE_COLUMN_TEMPLATE }}>
                        {tableDefinitionColumns.map((column) => (
                          <div key={column.key} className="solve-detail-grid-cell solve-detail-grid-cell-head">
                            <span>{column.label}</span>
                          </div>
                        ))}
                      </div>

                      {selectedTable.columns.length > 0 ? (
                        selectedTable.columns.map((column) => (
                          <div key={`${selectedTable.name}-${column.name}`} className="solve-detail-grid-row" style={{ gridTemplateColumns: SCHEMA_TABLE_COLUMN_TEMPLATE }}>
                            {renderSchemaGridCell(column.name, 'solve-detail-grid-cell-name')}
                            {renderSchemaGridCell(column.description)}
                            {renderSchemaGridCell(column.type || '-', 'solve-detail-grid-cell-type')}
                            {renderSchemaGridCell(formatColumnKey(column))}
                            {renderSchemaGridCell(column.reference ? `${column.reference.tableName}.${column.reference.columnName}` : '-')}
                          </div>
                        ))
                      ) : (
                        <p className="solve-detail-empty">{text('PROBLEM_DETAIL_TABLE_EMPTY_STATE', '표시할 테이블 정의가 없습니다.')}</p>
                      )}
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <p className="problem-create-empty">{text('PROBLEM_DETAIL_TABLE_EMPTY_STATE', '표시할 테이블 정의가 없습니다.')}</p>
            )}
          </div>
        ) : null}
      </section>

      <section className={`problem-create-section problem-create-schema-section ${collapsedSections.erd ? 'is-collapsed' : ''}`.trim()}>
        <div className="problem-create-section-header">
          <button
            type="button"
            className="problem-create-section-toggle"
            aria-label={collapsedSections.erd ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
            aria-expanded={!collapsedSections.erd}
            onClick={() => toggleSchemaSection('erd')}
          >
            <ChevronIcon collapsed={collapsedSections.erd} />
          </button>
          <h2 className="problem-create-section-title">{text('PROBLEM_DETAIL_ERD_SECTION_TITLE', 'ERD')}</h2>
          <div className="problem-create-section-actions" />
        </div>

        {!collapsedSections.erd ? (
          <div className="problem-create-section-body">
            {parsedSchema.tables.length > 0 ? (
              <div key={`problem-create-erd-${erdResetKey}`} className="solve-erd-frame problem-create-erd-frame">
                <ReactFlowDiagram tables={parsedSchema.tables} relations={parsedSchema.relations} className="solve-erd-diagram problem-create-erd-diagram" resetKey={erdResetKey} />
              </div>
            ) : (
              <p className="problem-create-empty">{text('PROBLEM_DETAIL_ERD_EMPTY_STATE', 'ERD를 만들 DDL이 없습니다.')}</p>
            )}
          </div>
        ) : null}
      </section>
    </div>
  );
}

function ProblemCreatePreviewGrid({ previewData }: { previewData: ProblemOutputPreviewData }) {
  if (previewData.columns.length === 0) {
    return <p className="problem-create-empty">{getUiTextValue('PROBLEM_CREATE_OUTPUT_EMPTY_STATE', '출력 예시가 아직 없습니다.')}</p>;
  }

  return (
    <div className="problem-create-preview-grid-shell">
      <div className="problem-create-preview-grid">
        <table>
          <thead>
            <tr>
              {previewData.columns.map((column) => (
                <th key={column}>{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {previewData.rows.map((row, rowIndex) => (
              <tr key={`preview-row-${rowIndex}`}>
                {row.map((value, columnIndex) => (
                  <td key={`preview-cell-${rowIndex}-${columnIndex}`}>{value == null ? '' : String(value)}</td>
                ))}
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={previewData.columns.length}>
                <div className="problem-create-preview-summary">
                  {formatExampleSummary(previewData.rowCount, previewData.visibleRows)}
                </div>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}

function ProblemCreateExampleFrame({ loading, label, children }: { loading: boolean; label: string; children: ReactNode }) {
  return (
    <div className={`problem-create-example-frame ${loading ? 'is-loading' : ''}`} aria-busy={loading}>
      <div className="problem-create-example-frame-content">
        {children}
      </div>
      {loading ? (
        <div className="problem-create-example-loading" role="status" aria-live="polite">
          <span className="problem-create-example-spinner" aria-hidden="true" />
          <span>{label}</span>
        </div>
      ) : null}
    </div>
  );
}

function ProblemCreateDataPreviewGrid({ previewData, schemaTables }: { previewData: ProblemDataPreviewData; schemaTables: ParsedSchemaTable[] }) {
  const [selectedTableName, setSelectedTableName] = useState('');
  const tableNamesSignature = previewData.tables.map((table) => table.name).join('|');

  useEffect(() => {
    setSelectedTableName((current) =>
      previewData.tables.some((table) => table.name === current)
        ? current
        : previewData.tables[0]?.name ?? '',
    );
  }, [previewData.tables, tableNamesSignature]);

  if (previewData.tables.length === 0) {
    return <p className="problem-create-empty">{getUiTextValue('PROBLEM_CREATE_DATA_EMPTY_STATE', '데이터 예시가 아직 없습니다.')}</p>;
  }

  const selectedTable = previewData.tables.find((table) => table.name === selectedTableName) ?? previewData.tables[0];
  const selectedTableDescription = schemaTables.find((table) => table.name === selectedTable.name)?.description ?? `${selectedTable.name} 데이터`;

  return (
    <div className="problem-create-data-preview">
      <div className="solve-detail-table-tab-row">
        {previewData.tables.map((table) => (
          <button
            key={table.name}
            type="button"
            className={`solve-bookmark-button ${selectedTable.name === table.name ? 'is-selected' : ''}`}
            aria-pressed={selectedTable.name === table.name}
            onClick={() => setSelectedTableName(table.name)}
          >
            {table.name}
          </button>
        ))}
      </div>

      <div className="solve-detail-table-stack">
        <div className="solve-detail-table-block problem-create-data-preview-block">
          <div className="solve-detail-table-block-header">
            <div className="solve-detail-table-block-copy">
              <p className="solve-detail-table-description">{selectedTableDescription}</p>
              <p className="solve-detail-table-name">{selectedTable.name}</p>
            </div>
          </div>

          <div className="problem-create-preview-grid-shell">
            <div className="problem-create-preview-grid">
              <table>
                <thead>
                  <tr>
                    {selectedTable.columns.map((column) => (
                      <th key={column}>{column}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {selectedTable.rows.map((row, rowIndex) => (
                    <tr key={`${selectedTable.name}-data-row-${rowIndex}`}>
                      {selectedTable.columns.map((column, columnIndex) => (
                        <td key={`${selectedTable.name}-${column}-${rowIndex}`}>{row[columnIndex] == null ? '' : String(row[columnIndex])}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={selectedTable.columns.length}>
                      <div className="problem-create-preview-summary">
                        {formatExampleSummary(selectedTable.totalRows, selectedTable.visibleRows)}
                      </div>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function ProblemCreateContent() {
  const { text } = useUiText();
  const pageRef = useRef<HTMLDivElement | null>(null);
  const heroSectionRef = useRef<HTMLElement | null>(null);
  const sectionRefs = useRef<SectionRefs>({
    condition: null,
    output: null,
    ddl: null,
    actualData: null,
    hiddenData: null,
    answerSql: null,
  });

  const heroState = useEditableDraft({ title: '', description: '' });
  const conditionState = useEditableDraft('');
  const outputState = useEditableDraft('');
  const ddlState = useEditableDraft<SourceDraft>({ postgresql: '', mysql: '' });
  const actualDataState = useEditableDraft<SourceDraft>({ postgresql: '', mysql: '' });
  const hiddenDataState = useEditableDraft<string[]>(['']);
  const answerSqlState = useEditableDraft('');

  const [existingProblemSet, setExistingProblemSet] = useState(true);
  const [existingProblem, setExistingProblem] = useState(false);
  const [problemSets, setProblemSets] = useState<ProblemSetSummary[]>([]);
  const [problemOptions, setProblemOptions] = useState<string[]>([]);
  const [selectedProblemSetId, setSelectedProblemSetId] = useState<string | null>(null);
  const [selectedProblemId, setSelectedProblemId] = useState<string | null>(null);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [loadedProblemSetDetail, setLoadedProblemSetDetail] = useState<ProblemSetDetailData>(EMPTY_PROBLEM_SET_DETAIL);
  const [loadedProblemDetail, setLoadedProblemDetail] = useState<ProblemDetailData | null>(null);
  const [isProblemSetLoading, setIsProblemSetLoading] = useState(false);
  const [isProblemLoading, setIsProblemLoading] = useState(false);
  const [isExamplePreviewLoading, setIsExamplePreviewLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [problemSetErrorMessage, setProblemSetErrorMessage] = useState('');
  const [problemErrorMessage, setProblemErrorMessage] = useState('');
  const [previewErrorMessage, setPreviewErrorMessage] = useState('');
  const [previewData, setPreviewData] = useState<ProblemOutputPreviewData>(EMPTY_PREVIEW_OUTPUT);
  const [dataPreviewErrorMessage, setDataPreviewErrorMessage] = useState('');
  const [dataPreviewData, setDataPreviewData] = useState<ProblemDataPreviewData>(EMPTY_DATA_PREVIEW);
  const [includedTableNames, setIncludedTableNames] = useState<string[]>([]);
  const [activeHiddenDataIndex, setActiveHiddenDataIndex] = useState(0);
  const [createProgressSteps, setCreateProgressSteps] = useState<ProblemCreateProgressStep[]>([]);
  const [popupState, setPopupState] = useState<PopupState>({ open: false, level: 2, message: '' });
  const [isHeroDescriptionCollapsed, setIsHeroDescriptionCollapsed] = useState(false);
  const [collapsedSections, setCollapsedSections] = useState<Record<SectionKey, boolean>>({
    condition: false,
    output: false,
    ddl: false,
    actualData: false,
    hiddenData: false,
    dataPreview: false,
    outputPreview: false,
    answerSql: false,
  });
  const isSavingRef = useRef(false);
  const createProgressTimeoutRef = useRef<number | null>(null);

  const currentDbms = useMemo<DbmsType>(() => {
    if (!existingProblemSet) {
      return selectedDbms;
    }

    return resolveScopedDbms(selectedProblemId ?? selectedProblemSetId);
  }, [existingProblemSet, selectedDbms, selectedProblemId, selectedProblemSetId]);

  const currentDraftProblemSetDdl = useMemo(
    () => (currentDbms === 'mysql' ? ddlState.appliedValue.mysql : ddlState.appliedValue.postgresql),
    [currentDbms, ddlState.appliedValue.mysql, ddlState.appliedValue.postgresql],
  );
  const currentFullProblemSetDdl = useMemo(
    () => existingProblemSet ? loadedProblemSetDetail.ddl : currentDraftProblemSetDdl,
    [currentDraftProblemSetDdl, existingProblemSet, loadedProblemSetDetail.ddl],
  );
  const currentFullActualData = useMemo(
    () => loadedProblemSetDetail.actualDataSql,
    [loadedProblemSetDetail.actualDataSql],
  );

  const availableTableNames = useMemo(() => {
    return getTableNamesFromDdl(currentFullProblemSetDdl);
  }, [currentFullProblemSetDdl]);

  useEffect(() => {
    isSavingRef.current = isSaving;
  }, [isSaving]);

  useEffect(() => {
    return subscribeSessionSocketMessages((message) => {
      if (!isSavingRef.current) {
        return;
      }

      if (!isProblemCreateProgressMessage(message)
          || typeof message.stepKey !== 'string'
          || typeof message.status !== 'string'
          || typeof message.message !== 'string') {
        return;
      }

      const status = message.status === 'success' ? 'success' : message.status === 'error' ? 'error' : 'running';
      const stepOrder = typeof message.stepOrder === 'number' ? message.stepOrder : Number.MAX_SAFE_INTEGER;
      const stepKey = String(message.stepKey);
      const progressMessage = String(message.message);
      setCreateProgressSteps((currentSteps) => upsertCreateProgressStep(currentSteps, {
        stepKey,
        status,
        message: progressMessage,
        stepOrder,
      }));

      if (status === 'error') {
        finishCreateProgressFailure(progressMessage);
        return;
      }

      if (status === 'success' && typeof message.problemId === 'string') {
        clearCreateProgressTimeout();
        isSavingRef.current = false;
        setIsSaving(false);
        navigate(`/problems/${encodeURIComponent(message.problemId)}`);
      }
    });
  }, []);

  useEffect(() => {
    return subscribeSessionSocketConnection((connected) => {
      if (!connected && isSavingRef.current) {
        finishCreateProgressFailure(getUiTextValue(
          'PROBLEM_CREATE_PROGRESS_CONNECTION_LOST_MESSAGE',
          '문제 생성 진행 상태 연결이 끊겼습니다. 다시 시도해 주세요.',
        ));
      }
    });
  }, []);

  useEffect(() => {
    return () => clearCreateProgressTimeout();
  }, []);

  useEffect(() => {
    if (availableTableNames.length === 0) {
      setIncludedTableNames([]);
      return;
    }

    if (existingProblemSet && existingProblem && loadedProblemDetail) {
      const problemDdl = loadedProblemDetail.ddl;
      const nextIncludedTableNames = getTableNamesFromDdl(problemDdl).filter((tableName) => availableTableNames.includes(tableName));
      const normalizedIncludedTableNames = nextIncludedTableNames.length > 0 ? nextIncludedTableNames : availableTableNames;
      setIncludedTableNames((current) => (arraysEqual(current, normalizedIncludedTableNames) ? current : normalizedIncludedTableNames));
      return;
    }

    setIncludedTableNames((current) => {
      const filtered = current.filter((tableName) => availableTableNames.includes(tableName));
      const nextIncludedTableNames = filtered.length > 0 ? filtered : availableTableNames;
      return arraysEqual(current, nextIncludedTableNames) ? current : nextIncludedTableNames;
    });
  }, [availableTableNames, currentDbms, loadedProblemDetail, existingProblem, existingProblemSet]);

  const scopedProblemDdl = useMemo(
    () => filterDdlByTableNames(currentFullProblemSetDdl, includedTableNames),
    [currentFullProblemSetDdl, includedTableNames],
  );
  const schemaPreviewDdl = !existingProblemSet && ddlState.isEditing
    ? (currentDbms === 'mysql' ? ddlState.draftValue.mysql : ddlState.draftValue.postgresql)
    : scopedProblemDdl;
  const storedSchemaMetadata = useMemo(
    () => parseSchemaMetadata(existingProblem && loadedProblemDetail ? loadedProblemDetail.schemaMetadata : ''),
    [existingProblem, loadedProblemDetail],
  );
  const schemaPreviewSchema = useMemo(
    () => existingProblem ? storedSchemaMetadata : parseSchemaDdl(schemaPreviewDdl),
    [existingProblem, schemaPreviewDdl, storedSchemaMetadata],
  );
  const schemaPreviewTables = schemaPreviewSchema.tables;

  const currentActualData = useMemo(() => {
    if (!existingProblemSet) {
      return currentDbms === 'mysql' ? actualDataState.appliedValue.mysql : actualDataState.appliedValue.postgresql;
    }

    return currentFullActualData;
  }, [actualDataState.appliedValue.mysql, actualDataState.appliedValue.postgresql, currentDbms, currentFullActualData, existingProblemSet]);

  useEffect(() => {
    const hiddenDataLength = hiddenDataState.isEditing ? hiddenDataState.draftValue.length : hiddenDataState.appliedValue.length;
    if (activeHiddenDataIndex >= hiddenDataLength) {
      setActiveHiddenDataIndex(Math.max(hiddenDataLength - 1, 0));
    }
  }, [activeHiddenDataIndex, hiddenDataState.appliedValue.length, hiddenDataState.draftValue.length, hiddenDataState.isEditing]);

  useEffect(() => {
    let cancelled = false;

    async function loadProblemSets() {
      try {
        const nextProblemSets = await fetchProblemSets();
        if (cancelled) return;

        setProblemSets(nextProblemSets);
        if (nextProblemSets.length === 0) {
          setExistingProblemSet(false);
          setSelectedProblemSetId(null);
          return;
        }

        setSelectedProblemSetId((current) => current ?? nextProblemSets[0].problemSetId);
      } catch (error) {
        if (!cancelled) {
          setExistingProblemSet(false);
          setSelectedProblemSetId(null);
          setProblemSetErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        }
      }
    }

    void loadProblemSets();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!existingProblemSet || !selectedProblemSetId) {
      setProblemOptions([]);
      setSelectedProblemId(null);
      setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
      setLoadedProblemDetail(null);
      setExistingProblem(false);
      setProblemSetErrorMessage('');
      setProblemErrorMessage('');
      return;
    }

    const targetProblemSetId = selectedProblemSetId;
    let cancelled = false;

    async function loadProblemManagementTargets() {
      setIsProblemSetLoading(true);
      setProblemSetErrorMessage('');

      try {
        const [nextDetail, nextProblemOptions] = await Promise.all([
          fetchProblemSetDetail(targetProblemSetId),
          fetchAdminProblemOptions(targetProblemSetId),
        ]);

        if (cancelled) return;

        setLoadedProblemSetDetail(nextDetail);
        setProblemOptions(nextProblemOptions);

        if (nextProblemOptions.length === 0) {
          setExistingProblem(false);
          setSelectedProblemId(null);
          resetProblemDrafts();
          return;
        }

        setExistingProblem(true);
        setSelectedProblemId((current) => (current != null && nextProblemOptions.includes(current) ? current : nextProblemOptions[0]));
      } catch (error) {
        if (!cancelled) {
          setLoadedProblemSetDetail(EMPTY_PROBLEM_SET_DETAIL);
          setProblemOptions([]);
          setExistingProblem(false);
          setSelectedProblemId(null);
          setProblemSetErrorMessage(error instanceof Error ? error.message : text('PROBLEM_CREATE_SET_DETAIL_FAIL_MESSAGE', '테이블셋 정보를 불러오지 못했습니다.'));
        }
      } finally {
        if (!cancelled) {
          setIsProblemSetLoading(false);
        }
      }
    }

    void loadProblemManagementTargets();
    return () => {
      cancelled = true;
    };
  }, [existingProblemSet, selectedProblemSetId]);

  useEffect(() => {
    if (!existingProblemSet || !existingProblem || !selectedProblemId) {
      setLoadedProblemDetail(null);
      setProblemErrorMessage('');
      return;
    }

    const targetProblemId = selectedProblemId;
    let cancelled = false;

    async function loadProblemDetailData() {
      setIsProblemLoading(true);
      setProblemErrorMessage('');

      try {
        const nextProblemDetail = await fetchProblemDetail(targetProblemId);
        if (cancelled) return;

        setLoadedProblemDetail(nextProblemDetail);
        heroState.replaceValue({ title: nextProblemDetail.title, description: nextProblemDetail.description });
        conditionState.replaceValue(nextProblemDetail.condition);
        outputState.replaceValue(nextProblemDetail.output);
        answerSqlState.replaceValue(nextProblemDetail.answerSql);
        setDataPreviewData({ rowLimit: 10, tables: parseProblemDataExample(nextProblemDetail.dataExample) });
        setPreviewData(parseProblemOutputExample(nextProblemDetail.outputExample));
        setDataPreviewErrorMessage('');
        setPreviewErrorMessage('');
      } catch (error) {
        if (!cancelled) {
          setLoadedProblemDetail(null);
          setProblemErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        }
      } finally {
        if (!cancelled) {
          setIsProblemLoading(false);
        }
      }
    }

    void loadProblemDetailData();
    return () => {
      cancelled = true;
    };
  }, [existingProblem, existingProblemSet, selectedProblemId]);

  useEffect(() => {
    if (existingProblemSet) {
      return;
    }

    resetProblemDrafts();
  }, [existingProblemSet]);

  const selectedProblemSetValue = existingProblemSet && selectedProblemSetId != null ? selectedProblemSetId : NEW_PROBLEM_SET_OPTION_VALUE;
  const selectedProblemValue = existingProblem && selectedProblemId != null ? selectedProblemId : NEW_PROBLEM_OPTION_VALUE;
  const immutableProblemSql = existingProblem;

  const missingFields = buildMissingFields({
    title: heroState.appliedValue.title,
    description: heroState.appliedValue.description,
    condition: conditionState.appliedValue,
    output: outputState.appliedValue,
    ddl: scopedProblemDdl,
    actualData: currentActualData,
    hiddenDataSqls: hiddenDataState.appliedValue,
    requireHiddenData: !existingProblem && !existingProblemSet,
    answerSql: answerSqlState.appliedValue,
  });

  const missingFieldLabels = missingFields.map((field) => field.label);
  const requiredFieldCount = existingProblem || existingProblemSet ? 7 : 8;
  const completedFieldCount = requiredFieldCount - missingFields.length;
  const isHeroMissing = missingFields.some((field) => field.key === 'title' || field.key === 'description');

  function clearCreateProgressTimeout() {
    if (createProgressTimeoutRef.current == null) {
      return;
    }

    window.clearTimeout(createProgressTimeoutRef.current);
    createProgressTimeoutRef.current = null;
  }

  function startCreateProgressTimeout() {
    clearCreateProgressTimeout();
    createProgressTimeoutRef.current = window.setTimeout(() => {
      finishCreateProgressFailure(getUiTextValue(
        'PROBLEM_CREATE_PROGRESS_TIMEOUT_MESSAGE',
        '문제 생성 진행 상태를 확인하지 못했습니다. 다시 시도해 주세요.',
      ));
    }, PROBLEM_CREATE_PROGRESS_TIMEOUT_MS);
  }

  function finishCreateProgressFailure(message: string) {
    clearCreateProgressTimeout();
    isSavingRef.current = false;
    setIsSaving(false);
    setCreateProgressSteps([]);
    setPopupState({ open: true, level: 2, message });
  }

  function resetProblemDrafts() {
    heroState.replaceValue({ title: '', description: '' });
    conditionState.replaceValue('');
    outputState.replaceValue('');
    hiddenDataState.replaceValue(['']);
    answerSqlState.replaceValue('');
    setActiveHiddenDataIndex(0);
    setIsHeroDescriptionCollapsed(false);
    setDataPreviewData(EMPTY_DATA_PREVIEW);
    setDataPreviewErrorMessage('');
    setPreviewData(EMPTY_PREVIEW_OUTPUT);
    setPreviewErrorMessage('');
  }

  function toggleSection(sectionKey: SectionKey) {
    setCollapsedSections((current) => ({
      ...current,
      [sectionKey]: !current[sectionKey],
    }));
  }

  function openSection(sectionKey: SectionKey) {
    setCollapsedSections((current) => ({
      ...current,
      [sectionKey]: false,
    }));
  }

  function scrollToElement(element: HTMLElement | null) {
    if (!element) {
      return;
    }

    window.requestAnimationFrame(() => {
      element.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    });
  }

  function scrollToMissingField(fieldKey: MissingFieldKey) {
    if (fieldKey === 'title' || fieldKey === 'description') {
      setIsHeroDescriptionCollapsed(false);
      if (!heroState.isEditing) {
        heroState.startEditing();
      }
      scrollToElement(heroSectionRef.current);
      return;
    }

    const sectionKey = fieldKey === 'ddl' ? 'ddl'
      : fieldKey === 'actualData' ? 'actualData'
      : fieldKey === 'hiddenData' ? 'hiddenData'
      : fieldKey === 'answerSql' ? 'answerSql'
      : fieldKey;

    openSection(sectionKey);
    scrollToElement(sectionRefs.current[sectionKey]);
  }

  function handleProblemSetSelectChange(nextValue: string) {
    if (nextValue === NEW_PROBLEM_SET_OPTION_VALUE) {
      setExistingProblemSet(false);
      setExistingProblem(false);
      setSelectedProblemSetId(null);
      setSelectedProblemId(null);
      return;
    }

    setExistingProblemSet(true);
    setSelectedProblemSetId(nextValue);
  }

  function handleProblemSelectChange(nextValue: string) {
    if (nextValue === NEW_PROBLEM_OPTION_VALUE) {
      setExistingProblem(false);
      setSelectedProblemId(null);
      resetProblemDrafts();
      return;
    }

    setExistingProblem(true);
    setSelectedProblemId(nextValue);
  }

  function updateHiddenDataDraft(index: number, nextValue: string) {
    hiddenDataState.setDraftValue((current) =>
      current.map((hiddenDataSql, hiddenDataIndex) => (hiddenDataIndex === index ? nextValue : hiddenDataSql)),
    );
  }

  function addHiddenDataDraft() {
    hiddenDataState.setDraftValue((current) => {
      if (current.length >= MAX_HIDDEN_DATA_SQL_COUNT) {
        return current;
      }

      setActiveHiddenDataIndex(current.length);
      return [...current, ''];
    });
  }

  async function handlePreviewExamples() {
    const previewMissingField = buildMissingFields({
      title: heroState.appliedValue.title || 'filled',
      description: heroState.appliedValue.description || 'filled',
      condition: conditionState.appliedValue || 'filled',
      output: outputState.appliedValue || 'filled',
      ddl: scopedProblemDdl,
      actualData: currentActualData,
      hiddenDataSqls: ['filled'],
      requireHiddenData: false,
      answerSql: answerSqlState.appliedValue,
    }).find((field) => field.key === 'ddl' || field.key === 'actualData' || field.key === 'answerSql');

    if (previewMissingField) {
      scrollToMissingField(previewMissingField.key);
      return;
    }

    setIsExamplePreviewLoading(true);
    setDataPreviewErrorMessage('');
    setPreviewErrorMessage('');

    try {
      const nextPreview = await previewProblemExamples({
        dbms: currentDbms,
        ddl: currentFullProblemSetDdl,
        problemDdl: scopedProblemDdl,
        actualDataSql: currentActualData.trim(),
        answerSql: answerSqlState.appliedValue.trim(),
      });
      setDataPreviewData(nextPreview.dataExample);
      setPreviewData(nextPreview.outputExample);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : text('PROBLEM_CREATE_EXAMPLE_PREVIEW_FAIL_MESSAGE', '예시를 생성하지 못했습니다.');
      setDataPreviewErrorMessage(errorMessage);
      setPreviewErrorMessage(errorMessage);
    } finally {
      setIsExamplePreviewLoading(false);
    }
  }

  async function handleCreateProblem() {
    if (missingFields.length > 0) {
      scrollToMissingField(missingFields[0].key);
      return;
    }

    const hiddenDataSqls = existingProblemSet
      ? []
      : hiddenDataState.appliedValue.map((hiddenDataSql) => hiddenDataSql.trim()).filter(Boolean);
    try {
      await connectSessionSocket();
    } catch {
      finishCreateProgressFailure(text(
        'PROBLEM_CREATE_PROGRESS_CONNECT_FAIL_MESSAGE',
        '문제 생성 진행 상태 연결에 실패했습니다. 다시 시도해 주세요.',
      ));
      return;
    }

    isSavingRef.current = true;
    setIsSaving(true);
    setCreateProgressSteps(createInitialCreateProgressSteps(hiddenDataSqls.length, existingProblemSet, existingProblem));
    startCreateProgressTimeout();

    try {
      await createProblem({
        title: heroState.appliedValue.title.trim(),
        description: heroState.appliedValue.description.trim(),
        condition: conditionState.appliedValue.trim(),
        output: outputState.appliedValue.trim(),
        ddl: currentFullProblemSetDdl.trim(),
        problemDdl: scopedProblemDdl.trim(),
        actualDataSql: currentActualData.trim(),
        hiddenDataSqls,
        answerSql: answerSqlState.appliedValue.trim(),
        problemSetId: existingProblemSet ? selectedProblemSetId ?? undefined : undefined,
        problemId: existingProblem ? selectedProblemId ?? undefined : undefined,
        dbms: currentDbms,
      });
    } catch (error) {
      finishCreateProgressFailure(
        error instanceof Error ? error.message : text('PROBLEM_CREATE_SAVE_FAIL_MESSAGE', '문제를 저장하지 못했습니다.'),
      );
    }
  }

  return (
    <>
      <div ref={pageRef} className="page-stack problem-create-page">
        <section ref={heroSectionRef} className="problem-create-hero">
          <div className="problem-create-number-row">
            <span className="solve-problem-number">{getProblemNumberLabel(existingProblemSet, existingProblem, selectedProblemSetId, selectedProblemId)}</span>
            <ProblemCreateSelectMenu
              value={selectedProblemSetValue}
              className="problem-create-select"
              options={[
                { value: NEW_PROBLEM_SET_OPTION_VALUE, label: text('PROBLEM_CREATE_NEW_SET_LABEL', '신규 테이블셋') },
                ...problemSets.map((problemSet) => ({
                  value: problemSet.problemSetId,
                  label: getProblemSetLabel(problemSet.problemSetId),
                })),
              ]}
              onChange={handleProblemSetSelectChange}
            />
            {!existingProblemSet ? (
              <ProblemCreateSelectMenu
                value={selectedDbms}
                className="problem-create-select problem-create-problem-select"
                options={[
                  { value: 'postgresql', label: text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
                  { value: 'mysql', label: text('COMMON_MYSQL_LABEL', 'MySQL') },
                ]}
                onChange={(nextValue) => setSelectedDbms(nextValue as DbmsType)}
              />
            ) : (
              <ProblemCreateSelectMenu
                value={selectedProblemValue}
                className="problem-create-select problem-create-problem-select"
                options={[
                  { value: NEW_PROBLEM_OPTION_VALUE, label: text('PROBLEM_CREATE_NEW_PROBLEM_LABEL', '신규 문제') },
                  ...problemOptions.map((problemId) => ({
                    value: problemId,
                    label: getProblemLabel(problemId),
                  })),
                ]}
                onChange={handleProblemSelectChange}
                disabled={problemOptions.length === 0}
              />
            )}
          </div>

          <section className={`problem-create-section problem-create-title-section ${isHeroDescriptionCollapsed ? 'is-collapsed' : ''} ${isHeroMissing ? 'is-missing' : ''}`.trim()}>
            <div className="problem-create-section-header">
              <button
                type="button"
                className="problem-create-section-toggle"
                aria-label={isHeroDescriptionCollapsed ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
                aria-expanded={!isHeroDescriptionCollapsed}
                aria-controls="problem-create-description-panel"
                onClick={() => setIsHeroDescriptionCollapsed((current) => !current)}
              >
                <ChevronIcon collapsed={isHeroDescriptionCollapsed} />
              </button>
              <h2 className="problem-create-section-title">{text('PROBLEM_CREATE_TITLE_DESCRIPTION_TITLE', '문제 제목/설명')}</h2>
              <div className="problem-create-section-actions">
                <InlineEditActions isEditing={heroState.isEditing} onEdit={heroState.startEditing} onCancel={heroState.cancelEditing} onConfirm={heroState.confirmEditing} />
              </div>
            </div>

            <div className="problem-create-title-section-body">
              <div className="problem-create-title-edit-row">
                {heroState.isEditing ? (
                  <input
                    className="problem-create-title-input"
                    name="problem-title"
                    autoComplete="off"
                    aria-label={text('PROBLEM_CREATE_TITLE_LABEL', '문제 제목')}
                    value={heroState.draftValue.title}
                    onChange={(event) => heroState.setDraftValue((current) => ({ ...current, title: event.target.value }))}
                    placeholder={text('PROBLEM_CREATE_TITLE_PLACEHOLDER', '문제 제목')}
                  />
                ) : (
                  <h1 className="solve-problem-title">
                    {heroState.appliedValue.title.trim() !== '' ? heroState.appliedValue.title : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_TITLE_LABEL', '문제 제목')}</span>}
                  </h1>
                )}
              </div>

              {!isHeroDescriptionCollapsed ? (
                <div id="problem-create-description-panel">
                  {heroState.isEditing ? (
                    <textarea
                      className="text-field problem-create-inline-textarea problem-create-description-editor"
                      name="problem-description"
                      autoComplete="off"
                      aria-label={text('PROBLEM_CREATE_DESCRIPTION_LABEL', '설명')}
                      value={heroState.draftValue.description}
                      onChange={(event) => heroState.setDraftValue((current) => ({ ...current, description: event.target.value }))}
                      placeholder={text('PROBLEM_CREATE_DESCRIPTION_PLACEHOLDER', '설명')}
                    />
                  ) : (
                    <p className="solve-problem-description">
                      {heroState.appliedValue.description.trim() !== '' ? heroState.appliedValue.description : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_DESCRIPTION_LABEL', '설명')}</span>}
                    </p>
                  )}
                </div>
              ) : null}
            </div>
          </section>
        </section>

        {existingProblemSet && isProblemSetLoading ? <p className="problem-create-info">{text('PROBLEM_CREATE_SET_LOADING_LABEL', '테이블셋 정보를 불러오는 중입니다.')}</p> : null}
        {existingProblemSet && problemSetErrorMessage ? <p className="problem-create-error">{problemSetErrorMessage}</p> : null}
        {existingProblem && isProblemLoading ? <p className="problem-create-info">{text('PROBLEM_CREATE_PROBLEM_LOADING_LABEL', '문제 정보를 불러오는 중입니다.')}</p> : null}
        {existingProblem && problemErrorMessage ? <p className="problem-create-error">{problemErrorMessage}</p> : null}

        {availableTableNames.length > 0 ? (
          <section className="problem-create-table-selector-card">
            <div className="problem-create-table-selector-header">
              <h2>{text('PROBLEM_CREATE_SET_SCOPE_TITLE', '테이블 범위')}</h2>
            </div>
            <div className="problem-create-table-chip-row">
              {availableTableNames.map((tableName) => (
                <label key={tableName} className="problem-create-table-chip">
                  <input
                    type="checkbox"
                    checked={includedTableNames.includes(tableName)}
                    disabled={immutableProblemSql}
                    onChange={() =>
                      setIncludedTableNames((current) =>
                        current.includes(tableName) ? current.filter((item) => item !== tableName) : [...current, tableName],
                      )
                    }
                  />
                  <span>{tableName}</span>
                </label>
              ))}
            </div>
          </section>
        ) : null}

        <ProblemCreateSection
          sectionKey="condition"
          title={text('PROBLEM_CREATE_CONDITION_LABEL', '조건')}
          collapsed={collapsedSections.condition}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'condition')}
          actions={<InlineEditActions isEditing={conditionState.isEditing} onEdit={conditionState.startEditing} onCancel={conditionState.cancelEditing} onConfirm={conditionState.confirmEditing} />}
          sectionRef={(node) => {
            sectionRefs.current.condition = node;
          }}
        >
          {conditionState.isEditing ? (
            <textarea className="text-field problem-create-inline-textarea" value={conditionState.draftValue} onChange={(event) => conditionState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_CONDITION_LABEL', '조건')} />
          ) : (
            <pre className="problem-create-code-preview">{conditionState.appliedValue.trim() !== '' ? conditionState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_CONDITION_LABEL', '조건')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="output"
          title={text('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명')}
          collapsed={collapsedSections.output}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'output')}
          actions={<InlineEditActions isEditing={outputState.isEditing} onEdit={outputState.startEditing} onCancel={outputState.cancelEditing} onConfirm={outputState.confirmEditing} />}
          sectionRef={(node) => {
            sectionRefs.current.output = node;
          }}
        >
          {outputState.isEditing ? (
            <textarea className="text-field problem-create-inline-textarea" value={outputState.draftValue} onChange={(event) => outputState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명')} />
          ) : (
            <pre className="problem-create-code-preview">{outputState.appliedValue.trim() !== '' ? outputState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_OUTPUT_LABEL', '출력 설명')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="ddl"
          title={text('PROBLEM_CREATE_DDL_LABEL', '테이블 정보 DDL')}
          collapsed={collapsedSections.ddl}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'ddl')}
          actions={
            !existingProblemSet ? (
              <InlineEditActions isEditing={ddlState.isEditing} onEdit={ddlState.startEditing} onCancel={ddlState.cancelEditing} onConfirm={ddlState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.ddl = node;
          }}
        >
          {!existingProblemSet && ddlState.isEditing ? (
            <textarea
              className="text-field problem-create-code-textarea"
              value={currentDbms === 'mysql' ? ddlState.draftValue.mysql : ddlState.draftValue.postgresql}
              onChange={(event) =>
                ddlState.setDraftValue((current) =>
                  currentDbms === 'mysql' ? { ...current, mysql: event.target.value } : { ...current, postgresql: event.target.value },
                )
              }
              placeholder={currentDbms === 'mysql' ? 'MySQL DDL' : 'PostgreSQL DDL'}
            />
          ) : (
            <pre className="problem-create-code-preview">{scopedProblemDdl.trim() !== '' ? scopedProblemDdl : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_DDL_LABEL', '테이블 정보 DDL')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSchemaPreview ddl={schemaPreviewDdl} schema={schemaPreviewSchema} />

        <ProblemCreateSection
          sectionKey="actualData"
          title={text('PROBLEM_CREATE_ACTUAL_DATA_OPEN_TITLE', '채점용 데이터 INSERT - Open')}
          collapsed={collapsedSections.actualData}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'actualData')}
          actions={
            !existingProblemSet ? (
              <InlineEditActions isEditing={actualDataState.isEditing} onEdit={actualDataState.startEditing} onCancel={actualDataState.cancelEditing} onConfirm={actualDataState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.actualData = node;
          }}
        >
          {!existingProblemSet && actualDataState.isEditing ? (
            <textarea
              className="text-field problem-create-code-textarea"
              value={currentDbms === 'mysql' ? actualDataState.draftValue.mysql : actualDataState.draftValue.postgresql}
              onChange={(event) =>
                actualDataState.setDraftValue((current) =>
                  currentDbms === 'mysql' ? { ...current, mysql: event.target.value } : { ...current, postgresql: event.target.value },
                )
              }
              placeholder={currentDbms === 'mysql' ? text('PROBLEM_CREATE_ACTUAL_DATA_MYSQL_PLACEHOLDER', 'MySQL 실제 채점 데이터 INSERT') : text('PROBLEM_CREATE_ACTUAL_DATA_POSTGRES_PLACEHOLDER', 'PostgreSQL 실제 채점 데이터 INSERT')}
            />
          ) : (
            <pre className="problem-create-code-preview">{currentActualData.trim() !== '' ? currentActualData : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_ACTUAL_DATA_OPEN_TITLE', '채점용 데이터 INSERT - Open')}</span>}</pre>
          )}
        </ProblemCreateSection>

        {!existingProblem && !existingProblemSet ? (
          <ProblemCreateSection
            sectionKey="hiddenData"
            title={text('PROBLEM_CREATE_HIDDEN_DATA_COMPACT_TITLE', '채점용 데이터 INSERT - Hidden')}
            collapsed={collapsedSections.hiddenData}
            onToggle={toggleSection}
            isMissing={missingFields.some((field) => field.key === 'hiddenData')}
            actions={<InlineEditActions isEditing={hiddenDataState.isEditing} onEdit={hiddenDataState.startEditing} onCancel={hiddenDataState.cancelEditing} onConfirm={hiddenDataState.confirmEditing} />}
            sectionRef={(node) => {
              sectionRefs.current.hiddenData = node;
            }}
          >
            <div className="problem-create-hidden-tab-row">
              {(hiddenDataState.isEditing ? hiddenDataState.draftValue : hiddenDataState.appliedValue).map((_, index) => (
                <button
                  key={`hidden-data-${index}`}
                  type="button"
                  className={`problem-create-hidden-tab ${activeHiddenDataIndex === index ? 'is-selected' : ''}`.trim()}
                  onClick={() => setActiveHiddenDataIndex(index)}
                >
                  Hidden {index + 1}
                </button>
              ))}
              {hiddenDataState.isEditing && hiddenDataState.draftValue.length < MAX_HIDDEN_DATA_SQL_COUNT ? (
                <button type="button" className="problem-create-hidden-add-button" onClick={addHiddenDataDraft}>
                  +
                </button>
              ) : null}
            </div>

            {hiddenDataState.isEditing ? (
              <textarea
                className="text-field problem-create-code-textarea"
                value={hiddenDataState.draftValue[activeHiddenDataIndex] ?? ''}
                onChange={(event) => updateHiddenDataDraft(activeHiddenDataIndex, event.target.value)}
                placeholder={text('PROBLEM_CREATE_HIDDEN_DATA_PLACEHOLDER', '채점용 데이터 - Hidden INSERT')}
              />
            ) : (
              <pre className="problem-create-code-preview">
                {(hiddenDataState.appliedValue[activeHiddenDataIndex] ?? '').trim() !== ''
                  ? hiddenDataState.appliedValue[activeHiddenDataIndex]
                  : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_HIDDEN_DATA_LABEL', '채점용 데이터 - Hidden')}</span>}
              </pre>
            )}
          </ProblemCreateSection>
        ) : null}

        <ProblemCreateSection
          sectionKey="answerSql"
          title={text('PROBLEM_CREATE_ANSWER_SQL_TITLE', '정답 SQL')}
          collapsed={collapsedSections.answerSql}
          onToggle={toggleSection}
          isMissing={missingFields.some((field) => field.key === 'answerSql')}
          actions={
            !immutableProblemSql ? (
              <InlineEditActions isEditing={answerSqlState.isEditing} onEdit={answerSqlState.startEditing} onCancel={answerSqlState.cancelEditing} onConfirm={answerSqlState.confirmEditing} />
            ) : null
          }
          sectionRef={(node) => {
            sectionRefs.current.answerSql = node;
          }}
        >
          {!immutableProblemSql && answerSqlState.isEditing ? (
            <textarea className="text-field problem-create-code-textarea problem-create-answer-textarea" value={answerSqlState.draftValue} onChange={(event) => answerSqlState.setDraftValue(event.target.value)} placeholder={text('PROBLEM_CREATE_ANSWER_SQL_LABEL', '정답 SQL')} />
          ) : (
            <pre className="problem-create-code-preview problem-create-answer-preview">{answerSqlState.appliedValue.trim() !== '' ? answerSqlState.appliedValue : <span className="problem-create-placeholder-text">{text('PROBLEM_CREATE_ANSWER_SQL_LABEL', '정답 SQL')}</span>}</pre>
          )}
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="dataPreview"
          title={text('PROBLEM_CREATE_DATA_SAMPLE_TITLE', '데이터 예시')}
          collapsed={collapsedSections.dataPreview}
          onToggle={toggleSection}
        >
          {dataPreviewErrorMessage ? <p className="problem-create-error">{dataPreviewErrorMessage}</p> : null}
          <ProblemCreateExampleFrame loading={isExamplePreviewLoading} label={text('PROBLEM_CREATE_EXAMPLE_GENERATING_LABEL', '예시 생성 중…')}>
            <ProblemCreateDataPreviewGrid previewData={dataPreviewData} schemaTables={schemaPreviewTables} />
          </ProblemCreateExampleFrame>
        </ProblemCreateSection>

        <ProblemCreateSection
          sectionKey="outputPreview"
          title={text('PROBLEM_CREATE_OUTPUT_SAMPLE_TITLE', '출력 예시')}
          collapsed={collapsedSections.outputPreview}
          onToggle={toggleSection}
        >
          {previewErrorMessage ? <p className="problem-create-error">{previewErrorMessage}</p> : null}
          <ProblemCreateExampleFrame loading={isExamplePreviewLoading} label={text('PROBLEM_CREATE_EXAMPLE_GENERATING_LABEL', '예시 생성 중…')}>
            <ProblemCreatePreviewGrid previewData={previewData} />
          </ProblemCreateExampleFrame>
        </ProblemCreateSection>
      </div>

      <div className="problem-create-sticky-bar">
        <div className="problem-create-sticky-status">
          {missingFields.length === 0 ? (
            <p>{text('PROBLEM_CREATE_COMPLETED_FIELDS_LABEL', { count: completedFieldCount, total: requiredFieldCount }, `필수 항목 ${completedFieldCount}/${requiredFieldCount} 완료`)}</p>
          ) : (
            <p>{text('PROBLEM_CREATE_MISSING_FIELDS_LABEL', { fields: missingFieldLabels.join(', ') }, `필수 항목 누락: ${missingFieldLabels.join(', ')}`)}</p>
          )}
        </div>
        <div className="problem-create-sticky-actions">
          <button type="button" className="btn secondary" onClick={() => void handlePreviewExamples()} disabled={isExamplePreviewLoading}>
            {isExamplePreviewLoading ? text('PROBLEM_CREATE_EXAMPLE_GENERATING_LABEL', '예시 생성 중…') : text('PROBLEM_CREATE_EXAMPLE_GENERATE_BUTTON', '예시 생성')}
          </button>
          <button type="button" className="btn primary problem-create-submit-button" onClick={() => void handleCreateProblem()} disabled={isSaving}>
            {isSaving ? text('PROBLEM_CREATE_SAVING_LABEL', '문제 저장 중') : text('PROBLEM_CREATE_CREATE_BUTTON', '문제 생성')}
          </button>
        </div>
      </div>

      <StatusPopup open={popupState.open} level={popupState.level} message={popupState.message} onConfirm={() => setPopupState((current) => ({ ...current, open: false }))} />
      {isSaving ? <ProblemCreateProgressPopup steps={createProgressSteps} /> : null}
    </>
  );
}

export default function ProblemCreatePage() {
  return <ProblemCreateContent />;
}
