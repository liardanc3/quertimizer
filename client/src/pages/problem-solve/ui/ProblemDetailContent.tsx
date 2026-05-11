import { memo, useEffect, useMemo, useRef, useState, type DragEvent, type ReactNode } from 'react';
import type { ProblemDataExampleTableData, ProblemDetailData, ProblemOutputExampleData } from '@/shared/api/problem-api';
import type { DbmsType } from '@/shared/api/domain';
import { getUiText, useUiText } from '@/shared/config/ui-text';
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

const DEFAULT_RESIZABLE_GRID_COLUMN_WIDTH = 104;
const DEFAULT_COLUMN_WEIGHTS = [2, 3.4, 1.6, 1, 2];
const MINIMUM_COLUMN_WEIGHTS = [1.2, 1.9, 1, 0.7, 1.2];
const ROW_COUNT_FORMATTER = new Intl.NumberFormat('ko-KR');

function parseSchemaMetadata(rawSchemaMetadata: string): ParsedDdl {
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

function parseLineItems(value: string) {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
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

function parseDataExample(rawDataExample: string): ProblemDataExampleTableData[] {
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

function parseOutputExample(rawOutputExample: string): ProblemOutputExampleData {
  if (rawOutputExample.trim() === '') {
    return { columns: [], rows: [], totalRows: 0, visibleRows: 0, rowLimit: 10 };
  }

  try {
    const parsed = JSON.parse(rawOutputExample) as {
      columns?: unknown;
      rows?: unknown;
      totalRows?: unknown;
      visibleRows?: unknown;
      rowLimit?: unknown;
    };
    const rows = normalizeExampleRows(parsed.rows);

    return {
      columns: Array.isArray(parsed.columns) ? parsed.columns.filter((column): column is string => typeof column === 'string') : [],
      rows,
      totalRows: typeof parsed.totalRows === 'number' ? parsed.totalRows : rows.length,
      visibleRows: typeof parsed.visibleRows === 'number' ? parsed.visibleRows : rows.length,
      rowLimit: typeof parsed.rowLimit === 'number' ? parsed.rowLimit : 10,
    };
  } catch {
    return { columns: [], rows: [], totalRows: 0, visibleRows: 0, rowLimit: 10 };
  }
}

function formatExampleSummary(totalRows: number, visibleRows: number) {
  return `… 총 ${ROW_COUNT_FORMATTER.format(totalRows)}행 중 ${ROW_COUNT_FORMATTER.format(visibleRows)}행 표시`;
}

function formatCellValue(value: ReactNode) {
  if (value == null) {
    return '';
  }

  if (typeof value === 'boolean') {
    return String(value);
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
    return fallbackWeights.map(() => DEFAULT_RESIZABLE_GRID_COLUMN_WIDTH);
  }

  return fallbackWeights.map((weight) => Math.max((weight / totalWeight) * containerWidth, DEFAULT_RESIZABLE_GRID_COLUMN_WIDTH));
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
        aria-label={getUiText('COMMON_COLUMN_RESIZE_LABEL', { label }, `${label} 너비 조절`)}
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

const ProblemDetailContent = memo(function ProblemDetailContent({
  detail,
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
  const { text } = useUiText();
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
  const parsedDdl = useMemo(() => parseSchemaMetadata(detail?.schemaMetadata ?? ''), [detail?.schemaMetadata]);
  const descriptionLines = useMemo(() => parseLineItems(detail?.description ?? ''), [detail?.description]);
  const conditionLines = useMemo(() => parseLineItems(detail?.condition ?? ''), [detail?.condition]);
  const outputLines = useMemo(() => parseLineItems(detail?.output ?? ''), [detail?.output]);
  const sampleTables = useMemo(() => {
    const availableTables = parseDataExample(detail?.dataExample ?? '');
    const allowedTableNames = new Set(parsedDdl.tables.map((table) => table.name));

    return availableTables.filter((table) => allowedTableNames.has(table.name));
  }, [detail?.dataExample, parsedDdl.tables]);
  const outputExample = useMemo(() => parseOutputExample(detail?.outputExample ?? ''), [detail?.outputExample]);
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
    () => openedSampleTableNames.map((tableName) => sampleTables.find((table) => table.name === tableName)).filter((table): table is ProblemDataExampleTableData => table != null),
    [openedSampleTableNames, sampleTables],
  );
  const tableDefinitionColumns: GridColumn[] = useMemo(
    () => [
      { key: 'name', label: text('PROBLEM_DETAIL_COLUMN_NAME_LABEL', '컬럼명'), bodyClassName: 'solve-detail-grid-cell-name' },
      { key: 'description', label: text('PROBLEM_DETAIL_COLUMN_DESCRIPTION_LABEL', '설명') },
      { key: 'type', label: text('PROBLEM_DETAIL_COLUMN_TYPE_LABEL', '타입'), bodyClassName: 'solve-detail-grid-cell-type' },
      { key: 'key', label: text('PROBLEM_DETAIL_COLUMN_KEY_LABEL', '키') },
      { key: 'reference', label: text('PROBLEM_DETAIL_COLUMN_REFERENCE_LABEL', '참조') },
    ],
    [text],
  );
  const outputExampleColumns: GridColumn[] = useMemo(
    () => outputExample.columns.map((column) => ({ key: column, label: column })),
    [outputExample.columns],
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
            title={text('PROBLEM_DETAIL_DRAG_REORDER_TITLE', '드래그해 순서 변경')}
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
          {dragState?.tableName === tableName ? <span className="solve-detail-drag-state">{text('PROBLEM_DETAIL_MOVING_STATE', '이동 중')}</span> : null}
          {isDropTarget && dragState?.tableName !== tableName ? <span className="solve-detail-drop-state">{text('PROBLEM_DETAIL_DROP_HERE_STATE', '여기에 놓기')}</span> : null}
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
          {descriptionContent ?? renderTextBlock(descriptionLines, text('PROBLEM_DETAIL_DESCRIPTION_EMPTY_STATE', '문제 설명이 없습니다.'))}
        </section>
      ) : null}

      {!hiddenSections?.table ? (
      <section className={`solve-detail-section solve-detail-section-table ${sectionClassNames?.table ?? ''}`.trim()}>
        <div className={`solve-detail-section-frame ${collapsedSections.table ? 'is-collapsed' : ''}`.trim()}>
          <div className="solve-detail-section-rail">
            <button
              type="button"
              className="solve-detail-section-divider-button"
              aria-label={collapsedSections.table ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
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
            <h2 className="solve-detail-section-title">{text('PROBLEM_DETAIL_TABLE_SECTION_TITLE', '테이블 정보')}</h2>
            {sectionTitleActions?.table}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.table}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label={text('COMMON_LAYOUT_RESET_LABEL', { label: text('PROBLEM_DETAIL_TABLE_SECTION_TITLE', '테이블 정보') }, '테이블 정보 레이아웃 초기화')}
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
                      emptyMessage={text('PROBLEM_DETAIL_TABLE_EMPTY_STATE', '표시할 테이블 정의가 없습니다.')}
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
              <p className="solve-detail-empty">{text('PROBLEM_DETAIL_TABLE_EMPTY_STATE', '표시할 테이블 정의가 없습니다.')}</p>
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
              aria-label={collapsedSections.erd ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
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
            <h2 className="solve-detail-section-title">{text('PROBLEM_DETAIL_ERD_SECTION_TITLE', 'ERD')}</h2>
            {sectionTitleActions?.erd}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.erd}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label={text('COMMON_LAYOUT_RESET_LABEL', { label: text('PROBLEM_DETAIL_ERD_SECTION_TITLE', 'ERD') }, 'ERD 레이아웃 초기화')}
              onClick={() => setErdResetKey((current) => current + 1)}
            >
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
              <p className="solve-detail-empty">{text('PROBLEM_DETAIL_ERD_EMPTY_STATE', 'ERD를 만들 DDL이 없습니다.')}</p>
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
              aria-label={collapsedSections.dataSample ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
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
            <h2 className="solve-detail-section-title">{text('PROBLEM_DETAIL_SAMPLE_SECTION_TITLE', '데이터 예시')}</h2>
            {sectionTitleActions?.dataSample}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.dataSample}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label={text('COMMON_LAYOUT_RESET_LABEL', { label: text('PROBLEM_DETAIL_SAMPLE_SECTION_TITLE', '데이터 예시') }, '데이터 예시 레이아웃 초기화')}
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
                    tableDefinition?.description ?? text('PROBLEM_DETAIL_SAMPLE_DEFAULT_TITLE', '예시 데이터'),
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
                    <div className="solve-detail-example-table">
                      <ResizableGrid
                        columns={table.columns.map((column) => ({ key: column, label: column }))}
                        rows={table.rows}
                        emptyMessage={text('PROBLEM_DETAIL_SAMPLE_EMPTY_STATE', '표시할 데이터 예시가 없습니다.')}
                        resetKey={gridResetKeys.dataSample}
                      />
                      <div className="solve-detail-example-summary">
                        {formatExampleSummary(table.totalRows, table.visibleRows)}
                      </div>
                    </div>,
                    sampleDragState,
                    'sample',
                  );
                })}
              </div>
            ) : (
              <p className="solve-detail-empty">{text('PROBLEM_DETAIL_SAMPLE_EMPTY_STATE', '표시할 데이터 예시가 없습니다.')}</p>
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
              aria-label={collapsedSections.condition ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
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
            <h2 className="solve-detail-section-title">{text('PROBLEM_DETAIL_CONDITION_SECTION_TITLE', '조건')}</h2>
            {sectionTitleActions?.condition}
          </div>
          <div className="solve-detail-section-header-actions">{sectionActions?.condition}</div>
        </div>
        {!collapsedSections.condition ? (
          <div className="solve-detail-section-body">
            {conditionContent ?? renderTextBlock(conditionLines, text('PROBLEM_DETAIL_CONDITION_EMPTY_STATE', '조건 정보가 없습니다.'))}
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
              aria-label={collapsedSections.output ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
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
            <h2 className="solve-detail-section-title">{text('PROBLEM_DETAIL_OUTPUT_SECTION_TITLE', '출력')}</h2>
            {sectionTitleActions?.output}
          </div>
          <div className="solve-detail-section-header-actions">{sectionActions?.output}</div>
        </div>
        {!collapsedSections.output ? (
          <div className="solve-detail-section-body">
            {outputContent ?? renderTextBlock(outputLines, text('PROBLEM_DETAIL_OUTPUT_EMPTY_STATE', '출력 정보가 없습니다.'))}
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
              aria-label={collapsedSections.outputSample ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
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
            <h2 className="solve-detail-section-title">{text('PROBLEM_DETAIL_OUTPUT_SAMPLE_SECTION_TITLE', '출력 예시')}</h2>
            {sectionTitleActions?.outputSample}
          </div>
          <div className="solve-detail-section-header-actions">
            {sectionActions?.outputSample}
            <button
              type="button"
              className="solve-detail-section-action solve-pane-action solve-pane-action-icon"
              aria-label={text('COMMON_LAYOUT_RESET_LABEL', { label: text('PROBLEM_DETAIL_OUTPUT_SAMPLE_SECTION_TITLE', '출력 예시') }, '출력 예시 레이아웃 초기화')}
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
                columns={outputExampleColumns}
                rows={outputExample.rows}
                emptyMessage={text('PROBLEM_DETAIL_OUTPUT_SAMPLE_EMPTY_STATE', '표시할 출력 예시가 없습니다.')}
                resetKey={gridResetKeys.outputSample}
              />
              <div className="solve-detail-example-summary">
                {formatExampleSummary(outputExample.totalRows, outputExample.visibleRows)}
              </div>
            </div>
          </div>
        ) : null}
          </div>
        </div>
      </section>
      ) : null}

    </div>
  );
});

export default ProblemDetailContent;
