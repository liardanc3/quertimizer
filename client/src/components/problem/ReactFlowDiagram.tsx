import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Background,
  BackgroundVariant,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  type Edge,
  type Node,
  type NodeProps,
  type NodeTypes,
  type ReactFlowInstance,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

interface ReactFlowDiagramTable {
  name: string;
  columns: {
    name: string;
    primaryKey?: boolean;
    foreignKey?: boolean;
  }[];
}

interface ReactFlowDiagramRelation {
  sourceTableName: string;
  sourceColumnName: string;
  targetTableName: string;
  targetColumnName: string;
}

interface ReactFlowDiagramProps {
  tables: ReactFlowDiagramTable[];
  relations: ReactFlowDiagramRelation[];
  className?: string;
  resetKey?: number;
}

interface TableNodeData extends Record<string, unknown> {
  name: string;
  columns: Array<{
    name: string;
    primaryKey?: boolean;
    foreignKey?: boolean;
  }>;
}

type FlowNode = Node<TableNodeData, 'table'>;

const NODE_WIDTH = 220;
const NODE_ROW_HEIGHT = 24;
const NODE_HEADER_HEIGHT = 34;
const NODE_SIDE_GAP = 88;

const TableNode = memo(function TableNode({ data }: NodeProps<FlowNode>) {
  return (
    <div className="solve-erd-node">
      <div className="solve-erd-node-header">{data.name}</div>

      <div className="solve-erd-node-body">
        {data.columns.map((column) => (
          <div key={column.name} className="solve-erd-node-row">
            <Handle
              id={`target:${column.name}`}
              type="target"
              position={Position.Left}
              className="solve-erd-node-handle"
              isConnectable={false}
            />

            <span className="solve-erd-node-row-name">{column.name}</span>

            {column.primaryKey || column.foreignKey ? (
              <span className="solve-erd-node-row-key">
                {column.primaryKey && column.foreignKey
                  ? 'PK, FK'
                  : column.primaryKey
                    ? 'PK'
                    : 'FK'}
              </span>
            ) : null}

            <Handle
              id={`source:${column.name}`}
              type="source"
              position={Position.Right}
              className="solve-erd-node-handle"
              isConnectable={false}
            />
          </div>
        ))}
      </div>
    </div>
  );
});

const nodeTypes: NodeTypes = {
  table: TableNode,
};

function buildNodes(tables: ReactFlowDiagramTable[]) {
  return tables.map<FlowNode>((table, index) => ({
    id: table.name,
    type: 'table',
    position: {
      x: index * (NODE_WIDTH + NODE_SIDE_GAP),
      y: 44,
    },
    draggable: false,
    selectable: false,
    data: {
      name: table.name,
      columns: table.columns.map((column) => ({
        name: column.name,
        primaryKey: column.primaryKey,
        foreignKey: column.foreignKey,
      })),
    },
    style: {
      width: NODE_WIDTH,
      height: NODE_HEADER_HEIGHT + table.columns.length * NODE_ROW_HEIGHT,
    },
  }));
}

function buildEdges(relations: ReactFlowDiagramRelation[]) {
  return relations.map<Edge>((relation, index) => ({
    id: `${relation.sourceTableName}-${relation.sourceColumnName}-${relation.targetTableName}-${relation.targetColumnName}-${index}`,
    source: relation.sourceTableName,
    target: relation.targetTableName,
    sourceHandle: `source:${relation.sourceColumnName}`,
    targetHandle: `target:${relation.targetColumnName}`,
    type: 'smoothstep',
    selectable: false,
    focusable: false,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 15,
      height: 15,
    },
    style: {
      stroke: '#94a3b8',
      strokeWidth: 1.2,
    },
  }));
}

export default function ReactFlowDiagram({ tables, relations, className, resetKey = 0 }: ReactFlowDiagramProps) {
  const reactFlowRef = useRef<ReactFlowInstance<FlowNode, Edge> | null>(null);
  const diagramRef = useRef<HTMLDivElement | null>(null);
  const [isDarkMode, setIsDarkMode] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)').matches : false,
  );
  const nodes = useMemo(() => buildNodes(tables), [tables]);
  const edges = useMemo(() => buildEdges(relations), [relations]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return undefined;
    }

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = () => {
      setIsDarkMode(mediaQuery.matches);
    };

    handleChange();

    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }

    mediaQuery.addListener(handleChange);
    return () => mediaQuery.removeListener(handleChange);
  }, []);

  const fitDiagram = useCallback(() => {
    if (!reactFlowRef.current || !diagramRef.current || nodes.length === 0) {
      return;
    }

    const containerWidth = diagramRef.current.clientWidth;
    const containerHeight = diagramRef.current.clientHeight;

    if (containerWidth <= 0 || containerHeight <= 0) {
      return;
    }

    const rawMinX = Math.min(...nodes.map((node) => node.position.x));
    const rawMinY = Math.min(...nodes.map((node) => node.position.y));
    const rawMaxX = Math.max(...nodes.map((node) => node.position.x + Number(node.style?.width ?? NODE_WIDTH)));
    const rawMaxY = Math.max(
      ...nodes.map((node) => node.position.y + Number(node.style?.height ?? NODE_HEADER_HEIGHT + NODE_ROW_HEIGHT)),
    );
    const boundsInsetX = 34;
    const boundsInsetY = 26;
    const minX = rawMinX - boundsInsetX;
    const minY = rawMinY - boundsInsetY;
    const maxX = rawMaxX + boundsInsetX;
    const maxY = rawMaxY + boundsInsetY;

    const boundsWidth = Math.max(maxX - minX, 1);
    const boundsHeight = Math.max(maxY - minY, 1);
    const leftPadding = Math.max(14, Math.min(44, containerWidth * 0.08));
    const rightPadding = 22;
    const verticalPadding = 30;
    const zoom = Math.max(
      0.12,
      Math.min(
        1.08,
        (containerWidth - leftPadding - rightPadding) / boundsWidth,
        (containerHeight - verticalPadding * 2) / boundsHeight,
      ),
    );
    const x = leftPadding - minX * zoom;
    const y = (containerHeight - boundsHeight * zoom) / 2 - minY * zoom;

    requestAnimationFrame(() => {
      reactFlowRef.current?.setViewport({ x, y, zoom }, { duration: 0 });
    });
  }, [nodes]);

  useEffect(() => {
    fitDiagram();
  }, [fitDiagram, resetKey, nodes, edges]);

  useEffect(() => {
    if (!diagramRef.current || nodes.length === 0) {
      return;
    }

    const resizeObserver = new ResizeObserver(() => {
      fitDiagram();
    });

    resizeObserver.observe(diagramRef.current);

    return () => {
      resizeObserver.disconnect();
    };
  }, [fitDiagram, nodes.length]);

  if (nodes.length === 0) {
    return <div className={className}>ERD를 만들 DDL이 없다.</div>;
  }

  return (
    <div className={className} ref={diagramRef}>
      <ReactFlow<FlowNode, Edge>
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onInit={(instance) => {
          reactFlowRef.current = instance;
          fitDiagram();
        }}
        minZoom={0.12}
        maxZoom={2}
        nodesDraggable={false}
        nodesConnectable={false}
        elementsSelectable={false}
        zoomOnScroll={false}
        zoomOnPinch={false}
        zoomOnDoubleClick={false}
        panOnScroll={false}
        panOnDrag
        preventScrolling={false}
        proOptions={{ hideAttribution: true }}
        className="solve-erd-reactflow"
      >
        <Background
          variant={BackgroundVariant.Dots}
          gap={18}
          size={1}
          color={isDarkMode ? 'rgba(71, 85, 105, 0.5)' : '#dbe6f1'}
        />
      </ReactFlow>

      <div className="solve-erd-zoom-controls">
        <button
          type="button"
          className="solve-erd-zoom-button"
          aria-label="ERD 확대"
          onClick={() => reactFlowRef.current?.zoomIn({ duration: 120 })}
        >
          +
        </button>
        <button
          type="button"
          className="solve-erd-zoom-button"
          aria-label="ERD 축소"
          onClick={() => reactFlowRef.current?.zoomOut({ duration: 120 })}
        >
          -
        </button>
      </div>
    </div>
  );
}
