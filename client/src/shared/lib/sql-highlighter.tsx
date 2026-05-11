import { Fragment, type ReactNode } from 'react';

export type SqlHighlightKind =
  | 'keyword'
  | 'explain-keyword'
  | 'table'
  | 'column'
  | 'string'
  | 'number'
  | 'comment'
  | 'function'
  | 'operator'
  | 'identifier';

export interface SqlHighlightToken {
  text: string;
  kind: SqlHighlightKind | null;
}

export interface SqlHighlightRange {
  start: number;
  end: number;
}

export const SQL_HIGHLIGHT_KEYWORDS = new Set([
  'SELECT',
  'FROM',
  'WHERE',
  'GROUP',
  'BY',
  'ORDER',
  'HAVING',
  'LIMIT',
  'OFFSET',
  'JOIN',
  'INNER',
  'LEFT',
  'RIGHT',
  'FULL',
  'OUTER',
  'ON',
  'AS',
  'AND',
  'OR',
  'NOT',
  'IN',
  'EXISTS',
  'BETWEEN',
  'LIKE',
  'IS',
  'NULL',
  'COUNT',
  'SUM',
  'AVG',
  'MIN',
  'MAX',
  'DISTINCT',
  'CASE',
  'WHEN',
  'THEN',
  'ELSE',
  'END',
  'WITH',
  'UNION',
  'ALL',
  'EXPLAIN',
  'ANALYZE',
  'ANALYSE',
  'CREATE',
  'TEMP',
  'TABLE',
  'INSERT',
  'INTO',
  'VALUES',
  'UPDATE',
  'SET',
  'DELETE',
  'INDEX',
  'DROP',
  'ALTER',
  'ADD',
  'PRIMARY',
  'KEY',
  'FOREIGN',
  'REFERENCES',
  'UNIQUE',
  'CHECK',
  'DEFAULT',
  'PUBLIC',
  'INTEGER',
  'VARCHAR',
  'TEXT',
  'TIMESTAMP',
  'DATE',
  'BOOLEAN',
  'DECIMAL',
  'NUMERIC',
  'BIGINT',
  'SMALLINT',
  'TRUE',
  'FALSE',
]);

export const SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS = new Set([
  'FROM',
  'JOIN',
  'INTO',
  'UPDATE',
  'TABLE',
  'INDEX',
  'ON',
]);

export function tokenizeSqlLine(line: string, tableNames: Set<string> = new Set(), columnNames: Set<string> = new Set()) {
  const tokens: SqlHighlightToken[] = [];
  const tokenPattern =
    /--.*$|'(?:''|[^'])*'|"(?:["]|[^"])*"|[A-Za-z_][A-Za-z0-9_$]*|\d+(?:\.\d+)?|<=|>=|<>|!=|==|[=<>+\-*/%]+|[(),.;]|\s+|./g;
  const lineTokens = Array.from(line.matchAll(tokenPattern), (match) => match[0]);
  let expectTable = false;

  for (let index = 0; index < lineTokens.length; index += 1) {
    const token = lineTokens[index];

    if (/^\s+$/.test(token)) {
      tokens.push({ text: token, kind: null });
      continue;
    }

    if (token.startsWith('--')) {
      tokens.push({ text: token, kind: 'comment' });
      break;
    }

    if (/^'(?:''|[^'])*'$/.test(token) || /^"(?:["]|[^"])*"$/.test(token)) {
      tokens.push({ text: token, kind: 'string' });
      expectTable = false;
      continue;
    }

    if (/^\d+(?:\.\d+)?$/.test(token)) {
      tokens.push({ text: token, kind: 'number' });
      continue;
    }

    if (/^[(),.;]$/.test(token) || /^[=<>+\-*/%]+$/.test(token)) {
      tokens.push({ text: token, kind: 'operator' });
      if (token !== ',') {
        expectTable = false;
      }
      continue;
    }

    if (/^[A-Za-z_][A-Za-z0-9_$]*$/.test(token)) {
      const upperToken = token.toUpperCase();
      const normalizedToken = token.toLowerCase();
      const previousMeaningfulToken = [...lineTokens.slice(0, index)].reverse().find((candidate) => !/^\s+$/.test(candidate));
      const nextMeaningfulToken = lineTokens.slice(index + 1).find((candidate) => !/^\s+$/.test(candidate));

      if (SQL_HIGHLIGHT_KEYWORDS.has(upperToken)) {
        tokens.push({
          text: token,
          kind: upperToken === 'EXPLAIN' || upperToken === 'ANALYZE' || upperToken === 'ANALYSE' ? 'explain-keyword' : 'keyword',
        });
        expectTable = SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS.has(upperToken);
        continue;
      }

      if (previousMeaningfulToken === '.') {
        tokens.push({ text: token, kind: 'column' });
        expectTable = false;
        continue;
      }

      if (expectTable || tableNames.has(normalizedToken)) {
        tokens.push({ text: token, kind: 'table' });
        expectTable = false;
        continue;
      }

      if (columnNames.has(normalizedToken)) {
        tokens.push({ text: token, kind: 'column' });
        expectTable = false;
        continue;
      }

      if (nextMeaningfulToken === '(') {
        tokens.push({ text: token, kind: 'function' });
        expectTable = false;
        continue;
      }

      tokens.push({ text: token, kind: 'identifier' });
      expectTable = false;
      continue;
    }

    tokens.push({ text: token, kind: null });
  }

  return tokens;
}

export function normalizeSqlHighlightRanges(ranges: SqlHighlightRange[], sqlLength: number) {
  const normalizedRanges = ranges
    .map((range) => ({
      start: Math.max(0, Math.min(range.start, sqlLength)),
      end: Math.max(0, Math.min(range.end, sqlLength)),
    }))
    .filter((range) => range.end > range.start)
    .sort((left, right) => left.start - right.start || left.end - right.end);

  return normalizedRanges.reduce<SqlHighlightRange[]>((mergedRanges, range) => {
    const previousRange = mergedRanges[mergedRanges.length - 1];

    if (!previousRange || range.start > previousRange.end) {
      mergedRanges.push({ ...range });
      return mergedRanges;
    }

    previousRange.end = Math.max(previousRange.end, range.end);
    return mergedRanges;
  }, []);
}

export function splitSqlTokenByHighlightRanges(
  text: string,
  tokenAbsoluteStart: number,
  highlightRanges: SqlHighlightRange[],
  errorRanges: SqlHighlightRange[] = [],
) {
  if (text.length === 0 || (highlightRanges.length === 0 && errorRanges.length === 0)) {
    return [
      {
        text,
        isHighlighted: false,
        isError: false,
      },
    ];
  }

  const tokenAbsoluteEnd = tokenAbsoluteStart + text.length;
  const overlappingHighlightRanges = highlightRanges.filter((range) => range.end > tokenAbsoluteStart && range.start < tokenAbsoluteEnd);
  const overlappingErrorRanges = errorRanges.filter((range) => range.end > tokenAbsoluteStart && range.start < tokenAbsoluteEnd);

  if (overlappingHighlightRanges.length === 0 && overlappingErrorRanges.length === 0) {
    return [
      {
        text,
        isHighlighted: false,
        isError: false,
      },
    ];
  }

  const boundaries = new Set([tokenAbsoluteStart, tokenAbsoluteEnd]);
  [...overlappingHighlightRanges, ...overlappingErrorRanges].forEach((range) => {
    boundaries.add(Math.max(range.start, tokenAbsoluteStart));
    boundaries.add(Math.min(range.end, tokenAbsoluteEnd));
  });
  const sortedBoundaries = [...boundaries].sort((left, right) => left - right);

  return sortedBoundaries.slice(0, -1)
    .map((segmentStart, boundaryIndex) => {
      const segmentEnd = sortedBoundaries[boundaryIndex + 1];
      return {
        text: text.slice(segmentStart - tokenAbsoluteStart, segmentEnd - tokenAbsoluteStart),
        isHighlighted: overlappingHighlightRanges.some((range) => range.start < segmentEnd && range.end > segmentStart),
        isError: overlappingErrorRanges.some((range) => range.start < segmentEnd && range.end > segmentStart),
      };
    })
    .filter((segment) => segment.text.length > 0);
}

export function renderHighlightedSql(
  sql: string,
  tableNames: Set<string> = new Set(),
  columnNames: Set<string> = new Set(),
  highlightRanges: SqlHighlightRange[] = [],
  errorRanges: SqlHighlightRange[] = [],
) {
  const normalizedSql = sql.replace(/\r\n/g, '\n');
  const normalizedHighlightRanges = normalizeSqlHighlightRanges(highlightRanges, normalizedSql.length);
  const normalizedErrorRanges = normalizeSqlHighlightRanges(errorRanges, normalizedSql.length);
  const lines = normalizedSql.split('\n');

  let lineAbsoluteStart = 0;

  return lines.map((line, lineIndex) => {
    const lineTokens = tokenizeSqlLine(line, tableNames, columnNames);
    let tokenOffset = 0;
    const renderedLine = lineTokens.flatMap((token, tokenIndex) => {
      const tokenAbsoluteStart = lineAbsoluteStart + tokenOffset;
      const tokenSegments = splitSqlTokenByHighlightRanges(token.text, tokenAbsoluteStart, normalizedHighlightRanges, normalizedErrorRanges);

      tokenOffset += token.text.length;

      return tokenSegments.map((segment, segmentIndex) => {
        const tokenContent =
          token.kind == null ? (
            segment.text
          ) : (
            <span className={`solve-sql-token is-${token.kind}`}>{segment.text}</span>
          );

        const errorContent = segment.isError
          ? <span className="solve-sql-error-underline">{tokenContent}</span>
          : tokenContent;

        return segment.isHighlighted ? (
          <span key={`token-${lineIndex}-${tokenIndex}-${segmentIndex}`} className="solve-sql-selection-fill">
            {errorContent}
          </span>
        ) : (
          <span key={`token-${lineIndex}-${tokenIndex}-${segmentIndex}`}>{errorContent}</span>
        );
      });
    });

    lineAbsoluteStart += line.length + 1;

    return (
      <Fragment key={`line-${lineIndex}`}>
        {renderedLine}
        {lineIndex < lines.length - 1 ? '\n' : null}
      </Fragment>
    );
  });
}

export function renderStaticHighlightedSql(sql: string, tableNames: Set<string> = new Set(), columnNames: Set<string> = new Set()): ReactNode {
  return renderHighlightedSql(sql, tableNames, columnNames);
}
