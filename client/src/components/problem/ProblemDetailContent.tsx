import { useEffect, useMemo, useRef, useState, type DragEvent, type ReactNode } from 'react';
import type { ProblemDetailData, ProblemOutputSampleData, ProblemSampleTableData } from '../../lib/problemApi';
import type { DbmsType } from '../../types/domain';
import ReactFlowDiagram from './ReactFlowDiagram';

interface ProblemDetailContentProps {
  detail: ProblemDetailData | null;
  selectedDbms: DbmsType;
  descriptionContent?: ReactNode;
  tableBeforeContent?: ReactNode;
  dataSampleBeforeContent?: ReactNode;
  conditionContent?: ReactNode;
  outputContent?: ReactNode;
  outputSampleBeforeContent?: ReactNode;
  sectionTitleActions?: Partial<Record<keyof CollapsedSectionState, ReactNode>>;
  sectionActions?: Partial<Record<keyof CollapsedSectionState, ReactNode>>;
  sectionClassNames?: Partial<Record<keyof CollapsedSectionState, string>>;
  hiddenSections?: Partial<Record<keyof CollapsedSectionState | 'description', boolean>>;
}

interface ParsedTableColumn {
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

interface ParsedTableRelation {
  sourceTableName: string;
  sourceColumnName: string;
  targetTableName: string;
  targetColumnName: string;
}

interface ParsedTable {
  name: string;
  description: string;
  columns: ParsedTableColumn[];
}

interface ParsedDdl {
  tables: ParsedTable[];
  relations: ParsedTableRelation[];
}

interface ColumnResizeState {
  columnIndex: number;
  startX: number;
  startWidths: number[];
}

interface DragState {
  tableName: string;
}

interface CollapsedSectionState {
  table: boolean;
  erd: boolean;
  dataSample: boolean;
  condition: boolean;
  output: boolean;
  outputSample: boolean;
}

interface GridColumn {
  key: string;
  label: string;
  bodyClassName?: string;
}

interface ResizableGridProps {
  columns: GridColumn[];
  rows: ReactNode[][];
  emptyMessage: string;
  initialWeights?: number[];
  minimumWeights?: number[];
  compact?: boolean;
  resetKey?: number;
}

const DEFAULT_COLUMN_WEIGHTS = [2, 3.4, 1.6, 1, 2];
const MINIMUM_COLUMN_WEIGHTS = [1.2, 1.9, 1, 0.7, 1.2];

function parseTableDefinitionSql(ddl: string): ParsedDdl {
  const createTablePattern = /CREATE TABLE\s+(?:[\w]+\.)?(\w+)\s*\(([\s\S]*?)\);/gi;
  const tableComments = parseTableComments(ddl);
  const columnComments = parseColumnComments(ddl);
  const tables: ParsedTable[] = [];
  const relations: ParsedTableRelation[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    const tableName = match[1];
    const lines = match[2]
      .split('\n')
      .map((line) => line.trim().replace(/,$/, ''))
      .filter(Boolean);

    const columns: ParsedTableColumn[] = [];

    lines.forEach((line) => {
      if (/^CONSTRAINT\s+/i.test(line) || /^FOREIGN KEY\s+/i.test(line)) {
        const foreignKeyMatch =
          line.match(/FOREIGN KEY\s+\((\w+)\)\s+REFERENCES\s+(?:[\w]+\.)?(\w+)\s+\((\w+)\)/i);

        if (!foreignKeyMatch) {
          return;
        }

        const [, sourceColumnName, targetTableName, targetColumnName] = foreignKeyMatch;
        const sourceColumn = columns.find((column) => column.name === sourceColumnName);

        if (sourceColumn) {
          sourceColumn.foreignKey = true;
          sourceColumn.reference = {
            tableName: targetTableName,
            columnName: targetColumnName,
          };
        }

        relations.push({
          sourceTableName: targetTableName,
          sourceColumnName: targetColumnName,
          targetTableName: tableName,
          targetColumnName: sourceColumnName,
        });
        return;
      }

      if (/^PRIMARY KEY\s*\(/i.test(line)) {
        const primaryKeyColumns = (line.match(/\(([^)]+)\)/)?.[1] ?? '')
          .split(',')
          .map((columnName) => columnName.trim())
          .filter(Boolean);

        primaryKeyColumns.forEach((primaryKeyColumnName) => {
          const targetColumn = columns.find((column) => column.name === primaryKeyColumnName);
          if (targetColumn) {
            targetColumn.primaryKey = true;
          }
        });
        return;
      }

      const columnMatch = line.match(/^(\w+)\s+(.+)$/);
      if (!columnMatch) {
        return;
      }

      const [, columnName, remainder] = columnMatch;
      const referenceMatch = remainder.match(/REFERENCES\s+(?:[\w]+\.)?(\w+)\s+\((\w+)\)/i);

      columns.push({
        name: columnName,
        type: extractColumnType(remainder),
        description: columnComments.get(`${tableName}.${columnName}`) ?? describeColumn(columnName),
        primaryKey: /PRIMARY KEY/i.test(remainder),
        foreignKey: referenceMatch != null,
        reference:
          referenceMatch == null
            ? undefined
            : {
                tableName: referenceMatch[1],
                columnName: referenceMatch[2],
              },
      });

      if (referenceMatch != null) {
        relations.push({
          sourceTableName: referenceMatch[1],
          sourceColumnName: referenceMatch[2],
          targetTableName: tableName,
          targetColumnName: columnName,
        });
      }
    });

    tables.push({
      name: tableName,
      description: tableComments.get(tableName) ?? `${formatIdentifier(tableName)} 테이블`,
      columns,
    });
  }

  return {
    tables,
    relations: dedupeRelations(relations),
  };
}

function parseTableComments(ddl: string) {
  const tableCommentPattern = /COMMENT ON TABLE\s+(?:[\w]+\.)?(\w+)\s+IS\s+'((?:''|[^'])*)';/gi;
  const tableComments = new Map<string, string>();
  let match: RegExpExecArray | null;

  while ((match = tableCommentPattern.exec(ddl)) != null) {
    tableComments.set(match[1], decodeSqlComment(match[2]));
  }

  return tableComments;
}

function parseColumnComments(ddl: string) {
  const columnCommentPattern = /COMMENT ON COLUMN\s+(?:[\w]+\.)?(\w+)\.(\w+)\s+IS\s+'((?:''|[^'])*)';/gi;
  const columnComments = new Map<string, string>();
  let match: RegExpExecArray | null;

  while ((match = columnCommentPattern.exec(ddl)) != null) {
    columnComments.set(`${match[1]}.${match[2]}`, decodeSqlComment(match[3]));
  }

  return columnComments;
}

function decodeSqlComment(value: string) {
  return value.replace(/''/g, "'");
}

function extractColumnType(remainder: string) {
  return remainder
    .replace(/\s+NOT NULL/gi, '')
    .replace(/\s+NULL/gi, '')
    .replace(/\s+PRIMARY KEY/gi, '')
    .replace(/\s+CHECK\s*\(.+$/gi, '')
    .replace(/\s+DEFAULT\s+.+$/gi, '')
    .replace(/\s+REFERENCES\s+.+$/gi, '')
    .trim();
}

function dedupeRelations(relations: ParsedTableRelation[]) {
  const relationMap = new Map<string, ParsedTableRelation>();

  relations.forEach((relation) => {
    const relationKey = [
      relation.sourceTableName,
      relation.sourceColumnName,
      relation.targetTableName,
      relation.targetColumnName,
    ].join(':');

    relationMap.set(relationKey, relation);
  });

  return Array.from(relationMap.values());
}

function describeColumn(columnName: string) {
  if (columnName.endsWith('_id')) {
    return `${formatIdentifier(columnName.replace(/_id$/, ''))} ID`;
  }

  if (columnName.endsWith('_at')) {
    return `${formatIdentifier(columnName.replace(/_at$/, ''))} 시각`;
  }

  if (columnName.endsWith('_date')) {
    return `${formatIdentifier(columnName.replace(/_date$/, ''))} 날짜`;
  }

  if (columnName.endsWith('_amount')) {
    return `${formatIdentifier(columnName.replace(/_amount$/, ''))} 금액`;
  }

  return formatIdentifier(columnName);
}

function formatIdentifier(value: string) {
  return value
    .split('_')
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ');
}

function parseLineItems(value: string) {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

function parseDataSampleSql(dataSampleSql: string): ProblemSampleTableData[] {
  try {
    const insertPattern = /INSERT INTO\s+(?:[\w]+\.)?(\w+)\s*\(([^)]+)\)\s*VALUES\s*([\s\S]*?);/gi;
    const sampleTableMap = new Map<string, ProblemSampleTableData>();
    let match: RegExpExecArray | null;

    while ((match = insertPattern.exec(dataSampleSql)) != null) {
      const tableName = match[1];
      const columns = match[2]
        .split(',')
        .map((column) => column.trim())
        .filter(Boolean);
      const rows = extractValueRows(match[3]).map((rowValue) => splitRowValues(rowValue).map(parseSqlValue));
      const existingTable = sampleTableMap.get(tableName);

      if (existingTable != null) {
        existingTable.rows.push(...rows);
        continue;
      }

      sampleTableMap.set(tableName, {
        name: tableName,
        columns,
        rows,
      });
    }

    return Array.from(sampleTableMap.values());
  } catch {
    return [];
  }
}

function extractValueRows(valuesSection: string) {
  const rowValues: string[] = [];
  let inQuote = false;
  let depth = 0;
  let rowStartIndex = -1;

  for (let index = 0; index < valuesSection.length; index += 1) {
    const currentCharacter = valuesSection[index];
    const nextCharacter = valuesSection[index + 1];

    if (currentCharacter === "'") {
      if (inQuote && nextCharacter === "'") {
        index += 1;
        continue;
      }

      inQuote = !inQuote;
      continue;
    }

    if (inQuote) {
      continue;
    }

    if (currentCharacter === '(') {
      if (depth === 0) {
        rowStartIndex = index + 1;
      }

      depth += 1;
      continue;
    }

    if (currentCharacter === ')') {
      depth -= 1;

      if (depth === 0 && rowStartIndex >= 0) {
        rowValues.push(valuesSection.slice(rowStartIndex, index));
        rowStartIndex = -1;
      }
    }
  }

  return rowValues;
}

function splitRowValues(rowValue: string) {
  const tokens: string[] = [];
  let inQuote = false;
  let tokenStartIndex = 0;

  for (let index = 0; index < rowValue.length; index += 1) {
    const currentCharacter = rowValue[index];
    const nextCharacter = rowValue[index + 1];

    if (currentCharacter === "'") {
      if (inQuote && nextCharacter === "'") {
        index += 1;
        continue;
      }

      inQuote = !inQuote;
      continue;
    }

    if (!inQuote && currentCharacter === ',') {
      tokens.push(rowValue.slice(tokenStartIndex, index).trim());
      tokenStartIndex = index + 1;
    }
  }

  tokens.push(rowValue.slice(tokenStartIndex).trim());
  return tokens;
}

function parseSqlValue(token: string): string | number | boolean | null {
  const normalizedToken = token.trim();

  if (/^null$/i.test(normalizedToken)) return null;
  if (/^true$/i.test(normalizedToken)) return true;
  if (/^false$/i.test(normalizedToken)) return false;
  if (/^-?\d+(?:\.\d+)?$/.test(normalizedToken)) return Number(normalizedToken);

  const typedLiteralMatch = normalizedToken.match(/^(?:DATE|TIMESTAMP)\s+'((?:''|[^'])*)'$/i);
  if (typedLiteralMatch) {
    return decodeSqlComment(typedLiteralMatch[1]);
  }

  const stringLiteralMatch = normalizedToken.match(/^'((?:''|[^'])*)'$/s);
  if (stringLiteralMatch) {
    return decodeSqlComment(stringLiteralMatch[1]);
  }

  return normalizedToken;
}

function parseOutputSampleCsv(outputSampleCsv: string): ProblemOutputSampleData {
  try {
    const lines = outputSampleCsv
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);

    if (lines.length === 0) {
      return { columns: [], rows: [] };
    }

    const [headerLine, ...dataLines] = lines;

    return {
      columns: parseCsvLine(headerLine),
      rows: dataLines.map((line) => parseCsvLine(line).map(parseCsvValue)),
    };
  } catch {
    return { columns: [], rows: [] };
  }
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

function formatCellValue(value: ReactNode) {
  if (value == null || typeof value === 'boolean') {
    return '';
  }

  return value;
}

function buildColumnTemplate(columnWidths: number[]) {
  return columnWidths.map((width) => `${width}px`).join(' ');
}

function resolveColumnWidths(containerWidth: number, columnCount: number, initialWeights?: number[]) {
  const fallbackWeights = initialWeights ?? Array.from({ length: columnCount }, () => 1);
  const totalWeight = fallbackWeights.reduce((sum, weight) => sum + weight, 0);

  if (containerWidth <= 0 || totalWeight <= 0) {
    return fallbackWeights.map(() => 160);
  }

  return fallbackWeights.map((weight) => (weight / totalWeight) * containerWidth);
}

function renderTextBlock(lines: string[], emptyMessage: string) {
  if (lines.length === 0) {
    return <p className="solve-detail-empty">{emptyMessage}</p>;
  }

  return (
    <div className="solve-detail-text-block">
      {lines.map((line, index) => (
        <p key={`${line}-${index}`}>{line}</p>
      ))}
    </div>
  );
}

function RefreshIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M12.85 6.1A4.95 4.95 0 0 0 4.7 3.8"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
      <path
        d="M4.65 1.95v2.7h2.7"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
      <path
        d="M3.15 9.9A4.95 4.95 0 0 0 11.3 12.2"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
      <path
        d="M11.35 14.05v-2.7h-2.7"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
    </svg>
  );
}

function formatColumnKey(column: ParsedTableColumn) {
  if (column.primaryKey && column.foreignKey) return 'PK, FK';
  if (column.primaryKey) return 'PK';
  if (column.foreignKey) return 'FK';
  return '-';
}

function CollapseChevronIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d={collapsed ? 'M4.2 6.2 8 10l3.8-3.8' : 'M4.2 9.8 8 6l3.8 3.8'}
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function ResizableGrid({ columns, rows, emptyMessage, initialWeights, minimumWeights, compact = false, resetKey = 0 }: ResizableGridProps) {
  const gridRef = useRef<HTMLDivElement | null>(null);
  const [columnWidths, setColumnWidths] = useState<number[]>([]);
  const [resizeState, setResizeState] = useState<ColumnResizeState | null>(null);
  const columnSignature = columns.map((column) => column.key).join('|');
  const initialWeightSignature = (initialWeights ?? []).join('|');

  useEffect(() => {
    if (!gridRef.current || columns.length === 0) {
      setColumnWidths([]);
      return;
    }

    const containerWidth = gridRef.current.getBoundingClientRect().width;
    setColumnWidths(resolveColumnWidths(containerWidth, columns.length, initialWeights));
  }, [columnSignature, columns.length, initialWeightSignature, resetKey]);

  useEffect(() => {
    if (!resizeState) {
      return;
    }

    const handleMouseMove = (event: MouseEvent) => {
      if (!gridRef.current) {
        return;
      }

      const containerWidth = gridRef.current.getBoundingClientRect().width;
      const totalWeight = (initialWeights ?? resizeState.startWidths.map(() => 1)).reduce((sum, weight) => sum + weight, 0);
      const minimumColumnWidths = (minimumWeights ?? resizeState.startWidths.map(() => 0.9)).map(
        (weight) => ((containerWidth > 0 ? containerWidth : 0) * weight) / totalWeight,
      );
      const deltaWidth = event.clientX - resizeState.startX;
      const leftWidth = resizeState.startWidths[resizeState.columnIndex];
      const rightWidth = resizeState.startWidths[resizeState.columnIndex + 1];
      const pairWidth = leftWidth + rightWidth;
      const nextLeftWidth = Math.min(
        Math.max(leftWidth + deltaWidth, minimumColumnWidths[resizeState.columnIndex]),
        pairWidth - minimumColumnWidths[resizeState.columnIndex + 1],
      );
      const nextWidths = [...resizeState.startWidths];

      nextWidths[resizeState.columnIndex] = nextLeftWidth;
      nextWidths[resizeState.columnIndex + 1] = pairWidth - nextLeftWidth;
      setColumnWidths(nextWidths);
    };

    const handleMouseUp = () => {
      setResizeState(null);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [initialWeights, minimumWeights, resizeState]);

  if (columns.length === 0) {
    return <p className="solve-detail-empty">{emptyMessage}</p>;
  }

  const resolvedColumnWidths =
    columnWidths.length === columns.length ? columnWidths : resolveColumnWidths(0, columns.length, initialWeights);
  const columnTemplate = buildColumnTemplate(resolvedColumnWidths);
  const rowWidth = `${resolvedColumnWidths.reduce((sum, width) => sum + width, 0)}px`;
  const gridClassName = compact ? 'solve-detail-grid-table is-compact' : 'solve-detail-grid-table';
  const rowClassName = compact ? 'solve-detail-grid-row is-compact' : 'solve-detail-grid-row';
  const cellClassName = compact ? 'solve-detail-grid-cell is-compact' : 'solve-detail-grid-cell';
  const headCellClassName = compact
    ? 'solve-detail-grid-cell solve-detail-grid-cell-head is-compact'
    : 'solve-detail-grid-cell solve-detail-grid-cell-head';

  const renderResizer = (columnIndex: number, label: string) => {
    if (columnIndex >= columns.length - 1) {
      return null;
    }

    return (
      <button
        type="button"
        className="solve-detail-grid-resizer"
        aria-label={`${label} 너비 조절`}
        onMouseDown={(event) => {
          event.preventDefault();
          setResizeState({
            columnIndex,
            startX: event.clientX,
            startWidths: resolvedColumnWidths,
          });
        }}
      />
    );
  };

  return (
    <div className={gridClassName} ref={gridRef}>
      <div className={`${rowClassName} solve-detail-grid-row-head`} style={{ gridTemplateColumns: columnTemplate, width: rowWidth }}>
        {columns.map((column, columnIndex) => (
          <div key={column.key} className={headCellClassName}>
            <span>{column.label}</span>
            {renderResizer(columnIndex, column.label)}
          </div>
        ))}
      </div>

      {rows.length > 0 ? (
        rows.map((row, rowIndex) => (
          <div key={`grid-row-${rowIndex}`} className={rowClassName} style={{ gridTemplateColumns: columnTemplate, width: rowWidth }}>
            {columns.map((column, columnIndex) => (
              <div
                key={`grid-row-${rowIndex}-${column.key}`}
                className={column.bodyClassName ? `${cellClassName} ${column.bodyClassName}` : cellClassName}
              >
                {formatCellValue(row[columnIndex])}
                {renderResizer(columnIndex, column.label)}
              </div>
            ))}
          </div>
        ))
      ) : (
        <p className="solve-detail-empty">{emptyMessage}</p>
      )}
    </div>
  );
}

export default function ProblemDetailContent({
  detail,
  selectedDbms,
  descriptionContent,
  tableBeforeContent,
  dataSampleBeforeContent,
  conditionContent,
  outputContent,
  outputSampleBeforeContent,
  sectionTitleActions,
  sectionActions,
  sectionClassNames,
  hiddenSections,
}: ProblemDetailContentProps) {
  const [collapsedSections, setCollapsedSections] = useState<CollapsedSectionState>({
    table: false,
    erd: false,
    dataSample: false,
    condition: false,
    output: false,
    outputSample: false,
  });
  const [gridResetKeys, setGridResetKeys] = useState({ table: 0, dataSample: 0, outputSample: 0 });
  const [erdResetKey, setErdResetKey] = useState(0);
  const [tableDragState, setTableDragState] = useState<DragState | null>(null);
  const [sampleDragState, setSampleDragState] = useState<DragState | null>(null);
  const [tableDropTargetName, setTableDropTargetName] = useState<string | null>(null);
  const [sampleDropTargetName, setSampleDropTargetName] = useState<string | null>(null);
  const [collapsedTableNames, setCollapsedTableNames] = useState<string[]>([]);
  const [collapsedSampleTableNames, setCollapsedSampleTableNames] = useState<string[]>([]);
  const [openedTableNames, setOpenedTableNames] = useState<string[]>([]);
  const [openedSampleTableNames, setOpenedSampleTableNames] = useState<string[]>([]);
  const selectedDdl = useMemo(
    () => {
      const preferredDdl = selectedDbms === 'oracle' ? detail?.ddlOracle ?? '' : detail?.ddlPostgresql ?? '';
      const fallbackDdl = selectedDbms === 'oracle' ? detail?.ddlPostgresql ?? '' : detail?.ddlOracle ?? '';

      return preferredDdl.trim() !== '' ? preferredDdl : fallbackDdl;
    },
    [detail?.ddlOracle, detail?.ddlPostgresql, selectedDbms],
  );
  const parsedDdl = useMemo(() => parseTableDefinitionSql(selectedDdl), [selectedDdl]);
  const descriptionLines = useMemo(() => parseLineItems(detail?.description ?? ''), [detail?.description]);
  const conditionLines = useMemo(() => parseLineItems(detail?.condition ?? ''), [detail?.condition]);
  const outputLines = useMemo(() => parseLineItems(detail?.output ?? ''), [detail?.output]);
  const selectedDataSampleSql = useMemo(
    () => {
      const preferredData = selectedDbms === 'oracle' ? detail?.dataOracle ?? '' : detail?.dataPostgresql ?? '';
      const fallbackData = selectedDbms === 'oracle' ? detail?.dataPostgresql ?? '' : detail?.dataOracle ?? '';

      return preferredData.trim() !== '' ? preferredData : fallbackData;
    },
    [detail?.dataOracle, detail?.dataPostgresql, selectedDbms],
  );
  const sampleTables = useMemo(() => {
    const availableTables = parseDataSampleSql(selectedDataSampleSql);
    const allowedTableNames = new Set(parsedDdl.tables.map((table) => table.name));

    return availableTables.filter((table) => allowedTableNames.has(table.name));
  }, [parsedDdl.tables, selectedDataSampleSql]);
  const outputSample = useMemo(() => parseOutputSampleCsv(detail?.outputSample ?? ''), [detail?.outputSample]);
  const tableNames = useMemo(() => parsedDdl.tables.map((table) => table.name), [parsedDdl.tables]);
  const sampleTableNames = useMemo(() => sampleTables.map((table) => table.name), [sampleTables]);

  useEffect(() => {
    if (tableNames.length === 0) {
      setOpenedTableNames([]);
      setCollapsedTableNames([]);
      return;
    }

    setOpenedTableNames((current) => {
      const next = current.filter((tableName) => tableNames.includes(tableName));
      return next.length > 0 ? next : tableNames;
    });
    setCollapsedTableNames((current) => {
      const next = current.filter((tableName) => tableNames.includes(tableName));
      return next.length > 0 ? next : tableNames.slice(1);
    });
  }, [tableNames]);

  useEffect(() => {
    if (sampleTableNames.length === 0) {
      setOpenedSampleTableNames([]);
      setCollapsedSampleTableNames([]);
      return;
    }

    setOpenedSampleTableNames((current) => {
      const next = current.filter((tableName) => sampleTableNames.includes(tableName));
      return next.length > 0 ? next : sampleTableNames;
    });
    setCollapsedSampleTableNames((current) => {
      const next = current.filter((tableName) => sampleTableNames.includes(tableName));
      return next.length > 0 ? next : sampleTableNames.slice(1);
    });
  }, [sampleTableNames]);

  const openedTables = useMemo(
    () => openedTableNames.map((tableName) => parsedDdl.tables.find((table) => table.name === tableName)).filter((table): table is ParsedTable => table != null),
    [openedTableNames, parsedDdl.tables],
  );
  const openedSampleTables = useMemo(
    () => openedSampleTableNames.map((tableName) => sampleTables.find((table) => table.name === tableName)).filter((table): table is ProblemSampleTableData => table != null),
    [openedSampleTableNames, sampleTables],
  );
  const tableDefinitionColumns: GridColumn[] = useMemo(
    () => [
      { key: 'name', label: '컬럼명', bodyClassName: 'solve-detail-grid-cell-name' },
      { key: 'description', label: '설명' },
      { key: 'type', label: '타입', bodyClassName: 'solve-detail-grid-cell-type' },
      { key: 'key', label: '키' },
      { key: 'reference', label: '참조' },
    ],
    [],
  );
  const outputSampleColumns: GridColumn[] = useMemo(
    () => outputSample.columns.map((column) => ({ key: column, label: column })),
    [outputSample.columns],
  );

  const toggleSection = (sectionKey: keyof CollapsedSectionState) => {
    setCollapsedSections((current) => ({ ...current, [sectionKey]: !current[sectionKey] }));
  };

  const resetGridLayout = (sectionKey: keyof typeof gridResetKeys) => {
    setGridResetKeys((current) => ({ ...current, [sectionKey]: current[sectionKey] + 1 }));
  };

  const toggleOpenedName = (tableName: string, setOpenedNames: React.Dispatch<React.SetStateAction<string[]>>, setCollapsedNames: React.Dispatch<React.SetStateAction<string[]>>, allNames: string[]) => {
    setOpenedNames((current) => {
      if (current.includes(tableName)) {
        setCollapsedNames((collapsed) => collapsed.filter((name) => name !== tableName));
        const next = current.filter((name) => name !== tableName);
        return next.length > 0 ? next : allNames.length > 0 ? [allNames[0]] : [];
      }

      setCollapsedNames((collapsed) => collapsed.filter((name) => name !== tableName));
      return [tableName, ...current.filter((name) => name !== tableName)];
    });
  };

  const toggleTableBlock = (tableName: string) => {
    toggleOpenedName(tableName, setOpenedTableNames, setCollapsedTableNames, tableNames);
  };

  const toggleSampleBlock = (tableName: string) => {
    toggleOpenedName(tableName, setOpenedSampleTableNames, setCollapsedSampleTableNames, sampleTableNames);
  };

  const toggleCollapsedName = (tableName: string, setCollapsedNames: React.Dispatch<React.SetStateAction<string[]>>) => {
    setCollapsedNames((current) =>
      current.includes(tableName) ? current.filter((name) => name !== tableName) : [...current, tableName],
    );
  };

  const moveOpenedName = (
    draggedTableName: string,
    targetTableName: string,
    setOpenedNames: React.Dispatch<React.SetStateAction<string[]>>,
  ) => {
    if (draggedTableName === targetTableName) {
      return;
    }

    setOpenedNames((current) => {
      const draggedIndex = current.indexOf(draggedTableName);
      const targetIndex = current.indexOf(targetTableName);
      if (draggedIndex < 0 || targetIndex < 0) {
        return current;
      }

      const next = [...current];
      const [dragged] = next.splice(draggedIndex, 1);
      next.splice(targetIndex, 0, dragged);
      return next;
    });
  };

  const renderTableBlock = (
    tableName: string,
    description: string,
    isCollapsed: boolean,
    onToggle: () => void,
    isDropTarget: boolean,
    onDrop: () => void,
    onDragStart: (event: DragEvent<HTMLSpanElement>) => void,
    onDragEnd: () => void,
    content: ReactNode,
    dragState: DragState | null,
    keyPrefix: string,
  ) => (
    <div
      key={`${keyPrefix}-${tableName}`}
      className={`solve-detail-table-block ${dragState?.tableName === tableName ? 'is-dragging' : ''} ${isDropTarget ? 'is-drop-target' : ''}`}
      onDragEnter={() => {
        if (dragState == null) {
          return;
        }

        if (keyPrefix === 'table') {
          setTableDropTargetName(tableName);
          return;
        }

        setSampleDropTargetName(tableName);
      }}
      onDragOver={(event) => {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
      }}
      onDrop={onDrop}
    >
      <div className="solve-detail-table-block-header">
        <div className="solve-detail-table-block-actions">
          <span
            className="solve-detail-table-drag-handle"
            draggable
            aria-hidden="true"
            title="드래그해 순서 변경"
            onDragStart={onDragStart}
            onDragEnd={onDragEnd}
          >
            ::
          </span>

          <button type="button" className="solve-detail-table-toggle" aria-expanded={!isCollapsed} onClick={onToggle}>
            <span className={`solve-detail-table-toggle-icon ${isCollapsed ? '' : 'is-open'}`}>{'>'}</span>
          </button>
        </div>

        <div className="solve-detail-table-block-copy">
          {dragState?.tableName === tableName ? <span className="solve-detail-drag-state">이동 중</span> : null}
          {isDropTarget && dragState?.tableName !== tableName ? <span className="solve-detail-drop-state">여기에 놓기</span> : null}
          <p className="solve-detail-table-description">{description}</p>
          <p className="solve-detail-table-name">{tableName}</p>
        </div>
      </div>

      {!isCollapsed ? content : null}
    </div>
  );

  return (
    <div className="solve-detail-content">
      {!hiddenSections?.description ? (
        <section className="solve-detail-section solve-detail-section-description">
          {descriptionContent ?? renderTextBlock(descriptionLines, '문제 설명이 없다.')}
        </section>
      ) : null}

      {!hiddenSections?.table ? (
      <section className={`solve-detail-section solve-detail-section-table ${sectionClassNames?.table ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame ${collapsedSections.table ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.table ? '펼치기' : '접기'}
              aria-expanded={!collapsedSections.table}
              onClick={() => toggleSection('table')}
            >
              <CollapseChevronIcon collapsed={collapsedSections.table} />
            </button>
            {!collapsedSections.table ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
          </div>
          <div className="solve-detail-section-main">
        <div className="solve-detail-section-header">
          <div className="solve-detail-section-title-row">
            <h2 className="solve-detail-section-title">테이블 정보</h2>
            {sectionTitleActions?.table}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.table}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label="테이블 정보 레이아웃 초기화"
              onClick={() => resetGridLayout('table')}
            >
              <RefreshIcon />
            </button>
          </div>
        </div>
        {!collapsedSections.table ? (
          <div className="solve-detail-section-body">
            {tableBeforeContent}
            <div className="solve-detail-table-tab-row">
              {parsedDdl.tables.map((table) => (
                <button
                  key={table.name}
                  type="button"
                  className={`solve-bookmark-button ${openedTableNames.includes(table.name) ? 'is-selected' : ''}`}
                  aria-pressed={openedTableNames.includes(table.name)}
                  onClick={() => toggleTableBlock(table.name)}
                >
                  {table.name}
                </button>
              ))}
            </div>

            {openedTables.length > 0 ? (
              <div className="solve-detail-table-stack">
                {openedTables.map((table) =>
                  renderTableBlock(
                    table.name,
                    table.description,
                    collapsedTableNames.includes(table.name),
                    () => toggleCollapsedName(table.name, setCollapsedTableNames),
                    tableDropTargetName === table.name && tableDragState?.tableName !== table.name,
                    () => {
                      if (!tableDragState) return;
                      moveOpenedName(tableDragState.tableName, table.name, setOpenedTableNames);
                      setTableDragState(null);
                      setTableDropTargetName(null);
                    },
                    (event) => {
                      event.dataTransfer.effectAllowed = 'move';
                      setTableDragState({ tableName: table.name });
                      setTableDropTargetName(table.name);
                    },
                    () => {
                      setTableDragState(null);
                      setTableDropTargetName(null);
                    },
                    <ResizableGrid
                      columns={tableDefinitionColumns}
                      rows={table.columns.map((column) => [
                        column.name,
                        column.description,
                        column.type || '-',
                        formatColumnKey(column),
                        column.reference ? `${column.reference.tableName}.${column.reference.columnName}` : '-',
                      ])}
                      emptyMessage="표시할 테이블 정의가 없다."
                      initialWeights={DEFAULT_COLUMN_WEIGHTS}
                      minimumWeights={MINIMUM_COLUMN_WEIGHTS}
                      resetKey={gridResetKeys.table}
                    />,
                    tableDragState,
                    'table',
                  ),
                )}
              </div>
            ) : (
              <p className="solve-detail-empty">표시할 테이블 정의가 없다.</p>
            )}
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}

      {!hiddenSections?.erd ? (
      <section className={`solve-detail-section solve-detail-section-erd ${sectionClassNames?.erd ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame is-erd ${collapsedSections.erd ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.erd ? '펼치기' : '접기'}
              aria-expanded={!collapsedSections.erd}
              onClick={() => toggleSection('erd')}
            >
              <CollapseChevronIcon collapsed={collapsedSections.erd} />
            </button>
            {!collapsedSections.erd ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
          </div>
          <div className="solve-detail-section-main">
        <div className="solve-detail-section-header">
          <div className="solve-detail-section-title-row">
            <h2 className="solve-detail-section-title">ERD</h2>
            {sectionTitleActions?.erd}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.erd}
            <button type="button" className="solve-detail-section-action solve-pane-action solve-pane-action-icon" aria-label="ERD 레이아웃 초기화" onClick={() => setErdResetKey((current) => current + 1)}>
              <RefreshIcon />
            </button>
          </div>
        </div>
        {!collapsedSections.erd ? (
          <div className="solve-detail-section-body">
            {parsedDdl.tables.length > 0 ? (
              <div key={`erd-frame-${erdResetKey}`} className="solve-erd-frame solve-detail-erd-frame">
                <ReactFlowDiagram tables={parsedDdl.tables} relations={parsedDdl.relations} className="solve-erd-diagram" resetKey={erdResetKey} />
              </div>
            ) : (
              <p className="solve-detail-empty">ERD를 만들 DDL이 없다.</p>
            )}
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}

      {!hiddenSections?.dataSample ? (
      <section className={`solve-detail-section solve-detail-section-data-sample ${sectionClassNames?.dataSample ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame ${collapsedSections.dataSample ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.dataSample ? '펼치기' : '접기'}
              aria-expanded={!collapsedSections.dataSample}
              onClick={() => toggleSection('dataSample')}
            >
              <CollapseChevronIcon collapsed={collapsedSections.dataSample} />
            </button>
            {!collapsedSections.dataSample ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
          </div>
          <div className="solve-detail-section-main">
        <div className="solve-detail-section-header">
          <div className="solve-detail-section-title-row">
            <h2 className="solve-detail-section-title">데이터 예시</h2>
            {sectionTitleActions?.dataSample}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.dataSample}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label="데이터 예시 레이아웃 초기화"
              onClick={() => resetGridLayout('dataSample')}
            >
              <RefreshIcon />
            </button>
          </div>
        </div>
        {!collapsedSections.dataSample ? (
          <div className="solve-detail-section-body">
            {dataSampleBeforeContent}
            <div className="solve-detail-table-tab-row">
              {sampleTables.map((table) => (
                <button
                  key={table.name}
                  type="button"
                  className={`solve-bookmark-button ${openedSampleTableNames.includes(table.name) ? 'is-selected' : ''}`}
                  aria-pressed={openedSampleTableNames.includes(table.name)}
                  onClick={() => toggleSampleBlock(table.name)}
                >
                  {table.name}
                </button>
              ))}
            </div>

            {openedSampleTables.length > 0 ? (
              <div className="solve-detail-table-stack">
                {openedSampleTables.map((table) => {
                  const tableDefinition = parsedDdl.tables.find((parsedTable) => parsedTable.name === table.name);

                  return renderTableBlock(
                    table.name,
                    tableDefinition?.description ?? '예시 데이터',
                    collapsedSampleTableNames.includes(table.name),
                    () => toggleCollapsedName(table.name, setCollapsedSampleTableNames),
                    sampleDropTargetName === table.name && sampleDragState?.tableName !== table.name,
                    () => {
                      if (!sampleDragState) return;
                      moveOpenedName(sampleDragState.tableName, table.name, setOpenedSampleTableNames);
                      setSampleDragState(null);
                      setSampleDropTargetName(null);
                    },
                    (event) => {
                      event.dataTransfer.effectAllowed = 'move';
                      setSampleDragState({ tableName: table.name });
                      setSampleDropTargetName(table.name);
                    },
                    () => {
                      setSampleDragState(null);
                      setSampleDropTargetName(null);
                    },
                    <ResizableGrid
                      columns={table.columns.map((column) => ({ key: column, label: column }))}
                      rows={table.rows}
                      emptyMessage="표시할 데이터 예시가 없다."
                      resetKey={gridResetKeys.dataSample}
                    />,
                    sampleDragState,
                    'sample',
                  );
                })}
              </div>
            ) : (
              <p className="solve-detail-empty">표시할 데이터 예시가 없다.</p>
            )}
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}

      {!hiddenSections?.condition ? (
      <section className={`solve-detail-section solve-detail-section-condition ${sectionClassNames?.condition ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame ${collapsedSections.condition ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.condition ? '펼치기' : '접기'}
              aria-expanded={!collapsedSections.condition}
              onClick={() => toggleSection('condition')}
            >
              <CollapseChevronIcon collapsed={collapsedSections.condition} />
            </button>
            {!collapsedSections.condition ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
          </div>
          <div className="solve-detail-section-main">
        <div className="solve-detail-section-header">
          <div className="solve-detail-section-title-row">
            <h2 className="solve-detail-section-title">조건</h2>
            {sectionTitleActions?.condition}
          </div>
          <div className="solve-detail-section-header-actions">{sectionActions?.condition}</div>
        </div>
        {!collapsedSections.condition ? (
          <div className="solve-detail-section-body">
            {conditionContent ?? renderTextBlock(conditionLines, '조건 정보가 없다.')}
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}

      {!hiddenSections?.output ? (
      <section className={`solve-detail-section solve-detail-section-output ${sectionClassNames?.output ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame ${collapsedSections.output ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.output ? '펼치기' : '접기'}
              aria-expanded={!collapsedSections.output}
              onClick={() => toggleSection('output')}
            >
              <CollapseChevronIcon collapsed={collapsedSections.output} />
            </button>
            {!collapsedSections.output ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
          </div>
          <div className="solve-detail-section-main">
        <div className="solve-detail-section-header">
          <div className="solve-detail-section-title-row">
            <h2 className="solve-detail-section-title">출력</h2>
            {sectionTitleActions?.output}
          </div>
          <div className="solve-detail-section-header-actions">{sectionActions?.output}</div>
        </div>
        {!collapsedSections.output ? (
          <div className="solve-detail-section-body">
            {outputContent ?? renderTextBlock(outputLines, '출력 정보가 없다.')}
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}

      {!hiddenSections?.outputSample ? (
      <section className={`solve-detail-section solve-detail-section-output-sample ${sectionClassNames?.outputSample ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame ${collapsedSections.outputSample ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.outputSample ? '펼치기' : '접기'}
              aria-expanded={!collapsedSections.outputSample}
              onClick={() => toggleSection('outputSample')}
            >
              <CollapseChevronIcon collapsed={collapsedSections.outputSample} />
            </button>
            {!collapsedSections.outputSample ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
          </div>
          <div className="solve-detail-section-main">
        <div className="solve-detail-section-header">
          <div className="solve-detail-section-title-row">
            <h2 className="solve-detail-section-title">출력 예시</h2>
            {sectionTitleActions?.outputSample}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.outputSample}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label="출력 예시 레이아웃 초기화"
              onClick={() => resetGridLayout('outputSample')}
            >
              <RefreshIcon />
            </button>
          </div>
        </div>
        {!collapsedSections.outputSample ? (
          <div className="solve-detail-section-body">
            {outputSampleBeforeContent}
            <div className="solve-detail-table-block solve-detail-output-example-block">
              <ResizableGrid
                columns={outputSampleColumns}
                rows={outputSample.rows}
                emptyMessage="표시할 출력 예시가 없다."
                resetKey={gridResetKeys.outputSample}
              />
            </div>
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}
    </div>
  );
}

